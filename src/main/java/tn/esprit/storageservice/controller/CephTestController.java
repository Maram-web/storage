package tn.esprit.storageservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            CreateBucketRequest request = CreateBucketRequest.builder()
                    .bucket(bucket)
                    .build();

            CreateBucketResponse response = s3Client.createBucket(request);
            return ResponseEntity.ok("✅ Bucket created successfully: " + response.location());
        } catch (S3Exception e) {
            if (e.awsErrorDetails().errorCode().equals("BucketAlreadyOwnedByYou")) {
                return ResponseEntity.ok("ℹ️ Bucket already exists.");
            }
            e.printStackTrace();
            return ResponseEntity.status(500).body("❌ Error creating bucket: " + e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("❌ Unexpected error: " + e.getMessage());
        }
    }
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        Map<String, String> result = new HashMap<>();
        System.out.println("🔥 Fichier reçu : " + file.getOriginalFilename());

        try {
            System.out.println("🔥 Fichier reçu : " + file.getOriginalFilename());

            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            long fileSize = file.getSize();

            if (!quotaService.canUpload(username, fileSize)) {
                result.put("message", quotaService.suggestUpgrade());
                return ResponseEntity.status(403).body(result);
            }

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .build();

            System.out.println("📦 Tentative d'envoi dans le bucket: " + bucket);
            s3Client.putObject(putRequest, RequestBody.fromBytes(file.getBytes()));
            quotaService.updateUsage(username, fileSize);

            result.put("message", "✅ Fichier uploadé avec succès !");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // log visible dans les pods
            System.out.println("❌ ERREUR lors de l'upload !");
            System.out.println("Exception: " + e.getClass().getSimpleName());
            System.out.println("Message: " + e.getMessage());
            e.printStackTrace();

            result.put("error", e.getClass().getSimpleName());
            result.put("message", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }





    @GetMapping("/list")
    public List<String> listFiles() {
        return s3Client.listObjectsV2(ListObjectsV2Request.builder().bucket(bucket).build())
                .contents()
                .stream()
                .map(S3Object::key)
                .toList();
    }
    @GetMapping("/quota/remaining")
    public String getRemainingQuotaFormatted(HttpServletRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        long remainingBytes = quotaService.getRemainingQuota(username);

        double remainingKB = remainingBytes / 1024.0;
        double remainingMB = remainingBytes / (1024.0 * 1024.0);

        return String.format("💾 Quota restant : %.2f Mo = %.2f Ko", remainingMB, remainingKB);
    } // hattinah huni khatr aandu aalaka b uploads f ceph



    @RestControllerAdvice
    public class GlobalExceptionHandler {

        @ExceptionHandler(Exception.class)
        public ResponseEntity<Map<String, String>> handle(Exception e) {
            e.printStackTrace(); // Tu verras l’erreur réelle dans les logs
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception ex) {
        System.out.println("💥 ERREUR GLOBALE CAPTÉE !");
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur serveur: " + ex.getMessage());
    }


}
