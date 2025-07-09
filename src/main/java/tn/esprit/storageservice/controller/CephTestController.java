package tn.esprit.storageservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    public ResponseEntity<String> createBucket(@RequestParam String name) {
        try {
            log.info("🪣 Tentative de création du bucket : {}", name);
            CreateBucketRequest request = CreateBucketRequest.builder().bucket(name).build();
            CreateBucketResponse response = s3Client.createBucket(request);
            return ResponseEntity.ok("✅ Bucket créé avec succès: " + response.location());
        } catch (S3Exception e) {
            if ("BucketAlreadyOwnedByYou".equals(e.awsErrorDetails().errorCode())) {
                log.warn("⚠️ Bucket déjà existant : {}", name);
                return ResponseEntity.ok("ℹ️ Le bucket existe déjà.");
            }
            log.error("❌ Erreur S3: {}", e.awsErrorDetails().errorMessage(), e);
            return ResponseEntity.status(500).body("Erreur S3: " + e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            log.error("❌ Erreur inattendue lors de la création du bucket", e);
            return ResponseEntity.status(500).body("Erreur inconnue: " + e.getMessage());
        }
    }

    @PostMapping("/{bucket}/upload")
    public ResponseEntity<String> uploadToBucket(@PathVariable String bucket,
                                                 @RequestParam("file") MultipartFile file,
                                                 Authentication auth) {
        try {
            String username = auth.getName();
            long fileSize = file.getSize();

            if (!quotaService.canUpload(username, fileSize)) {
                return ResponseEntity.status(403).body("🚫 Quota dépassé.");
            }

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(file.getBytes()));
            quotaService.updateUsage(username, fileSize);

            return ResponseEntity.ok("✅ Fichier uploadé dans le bucket : " + bucket);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("❌ Erreur : " + e.getMessage());
        }
    }
    @GetMapping("/{bucket}/files")
    public ResponseEntity<List<String>> listFilesInBucket(@PathVariable String bucket) {
        List<String> keys = s3Client.listObjectsV2(ListObjectsV2Request.builder().bucket(bucket).build())
                .contents()
                .stream()
                .map(S3Object::key)
                .toList();
        return ResponseEntity.ok(keys);
    }
    @GetMapping("/{bucket}/files/{filename}")
    public ResponseEntity<byte[]> downloadFileFromBucket(@PathVariable String bucket,
                                                         @PathVariable String filename) {
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(filename)
                    .build();

            byte[] fileBytes = s3Client.getObjectAsBytes(getRequest).asByteArray();

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=" + filename)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(fileBytes);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(null);
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
