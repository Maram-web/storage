package tn.esprit.storageservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import tn.esprit.storageservice.security.JwtService;
import tn.esprit.storageservice.service.QuotaService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3")
public class CephTestController {

    private final S3Client s3Client;
    private final QuotaService quotaService;
    private final JwtService jwtService;

    private String bucket;

    @PostMapping("/bucket/create")
    public ResponseEntity<Map<String, String>> createBucket(@RequestParam String name, HttpServletRequest request) {
        try {
            String token = jwtService.extractTokenFromRequest(request);
            String username = jwtService.extractUsername(token);

            String bucketName = username + "-" + name;
            CreateBucketRequest createRequest = CreateBucketRequest.builder().bucket(bucketName).build();
            s3Client.createBucket(createRequest);
            quotaService.initializeQuota(username, bucketName);

            return ResponseEntity.ok(Map.of("message", "✅ Bucket créé : " + bucketName));
        } catch (S3Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "❌ Erreur S3: " + e.awsErrorDetails().errorMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "❌ Erreur: " + e.getMessage()));
        }
    }

    @GetMapping("/buckets")
    public ResponseEntity<List<String>> listUserBuckets(Authentication auth) {
        String username = auth.getName();
        List<String> buckets = s3Client.listBuckets().buckets().stream()
                .map(Bucket::name)
                .filter(name -> name.startsWith(username + "-"))
                .toList();
        return ResponseEntity.ok(buckets);
    }

    @PostMapping("/{bucket}/upload")
    public ResponseEntity<Map<String, String>> uploadToBucket(@PathVariable String bucket,
                                                              @RequestParam("file") MultipartFile file,
                                                              Authentication auth) {
        try {
            String username = auth.getName();
            long fileSize = file.getSize();

            if (!quotaService.canUpload(username, bucket, fileSize)) {
                return ResponseEntity.status(403).body(Map.of("error", "🚫 Quota dépassé."));
            }

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(file.getBytes()));
            quotaService.updateUsage(username, bucket, fileSize);

            return ResponseEntity.ok(Map.of("message", "✅ Fichier uploadé dans le bucket : " + bucket));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "❌ Erreur : " + e.getMessage()));
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

    @GetMapping("/{bucket}/quota/remaining/formatted")
    public ResponseEntity<Map<String, String>> getRemainingQuotaFormatted(@PathVariable String bucket) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        long remainingBytes = quotaService.getRemainingQuota(username, bucket);
        double remainingKB = remainingBytes / 1024.0;
        double remainingMB = remainingBytes / (1024.0 * 1024.0);

        String message = String.format("💾 Quota restant pour %s dans le bucket %s : %.2f Mo (%.2f Ko)",
                username, bucket, remainingMB, remainingKB);
        log.info(message);
        return ResponseEntity.ok(Map.of("quota", message));
    }

    @GetMapping("/{bucket}/quota/remaining")
    public ResponseEntity<Map<String, String>> getRemainingQuotaPerBucket(@PathVariable String bucket, Authentication auth) {
        String username = auth.getName();
        long remainingBytes = quotaService.getRemainingQuota(username, bucket);
        double remainingMB = remainingBytes / (1024.0 * 1024.0);
        return ResponseEntity.ok(Map.of("quota", String.format("%.2f Mo", remainingMB)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception ex) {
        log.error("💥 ERREUR NON GÉRÉE dans CephTestController !", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Erreur serveur : " + ex.getMessage()));
    }
}
