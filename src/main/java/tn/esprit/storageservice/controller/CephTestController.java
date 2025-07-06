package tn.esprit.storageservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import tn.esprit.storageservice.service.QuotaService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/s3")
public class CephTestController {

    private final S3Client s3Client;
    private final QuotaService quotaService;

    @Value("${ceph.s3.bucket}")
    private String bucket;

    @PostMapping("/bucket/create")
    public ResponseEntity<String> createBucket() {
        try {
            log.info("🪣 Tentative de création du bucket : {}", bucket);
            CreateBucketRequest request = CreateBucketRequest.builder().bucket(bucket).build();
            CreateBucketResponse response = s3Client.createBucket(request);
            return ResponseEntity.ok("✅ Bucket créé avec succès: " + response.location());
        } catch (S3Exception e) {
            if ("BucketAlreadyOwnedByYou".equals(e.awsErrorDetails().errorCode())) {
                log.warn("⚠️ Bucket déjà existant : {}", bucket);
                return ResponseEntity.ok("ℹ️ Le bucket existe déjà.");
            }
            log.error("❌ Erreur S3: {}", e.awsErrorDetails().errorMessage(), e);
            return ResponseEntity.status(500).body("Erreur S3: " + e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            log.error("❌ Erreur inattendue lors de la création du bucket", e);
            return ResponseEntity.status(500).body("Erreur inconnue: " + e.getMessage());
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        Map<String, String> result = new HashMap<>();
        String filename = file.getOriginalFilename();

        try {
            log.info("📥 Upload du fichier reçu : {}", filename);

            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            long fileSize = file.getSize();
            log.info("👤 Utilisateur : {}, Taille du fichier : {} octets", username, fileSize);

            if (!quotaService.canUpload(username, fileSize)) {
                result.put("message", quotaService.suggestUpgrade());
                return ResponseEntity.status(403).body(result);
            }

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(filename)
                    .contentType(file.getContentType())
                    .build();

            log.info("🚀 Upload vers le bucket Ceph: {}", bucket);
            s3Client.putObject(putRequest, RequestBody.fromBytes(file.getBytes()));

            quotaService.updateUsage(username, fileSize);
            result.put("message", "✅ Fichier uploadé avec succès !");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Erreur pendant l'upload du fichier: {}", filename, e);
            result.put("error", e.getClass().getSimpleName());
            result.put("message", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    @GetMapping("/list")
    public List<String> listFiles() {
        log.info("📃 Listing des fichiers du bucket {}", bucket);
        return s3Client.listObjectsV2(ListObjectsV2Request.builder().bucket(bucket).build())
                .contents()
                .stream()
                .map(S3Object::key)
                .toList();
    }

    @GetMapping("/quota/remaining")
    public String getRemainingQuotaFormatted() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        long remainingBytes = quotaService.getRemainingQuota(username);
        double remainingKB = remainingBytes / 1024.0;
        double remainingMB = remainingBytes / (1024.0 * 1024.0);

        String message = String.format("💾 Quota restant pour %s : %.2f Mo (%.2f Ko)", username, remainingMB, remainingKB);
        log.info(message);
        return message;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception ex) {
        log.error("💥 ERREUR NON GÉRÉE dans CephTestController !", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erreur serveur : " + ex.getMessage());
    }
}
