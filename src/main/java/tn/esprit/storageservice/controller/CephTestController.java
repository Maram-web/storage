package tn.esprit.storageservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import tn.esprit.storageservice.service.QuotaService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.io.IOException;
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

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        long fileSize = file.getSize();

        if (!quotaService.canUpload(username, fileSize)) {
            return ResponseEntity.status(403).body(Map.of("message", quotaService.suggestUpgrade()));
        }

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(file.getOriginalFilename())
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putRequest, RequestBody.fromBytes(file.getBytes()));
        quotaService.updateUsage(username, fileSize);

        return ResponseEntity.ok(Map.of("message", "✅ File uploaded to Ceph S3!"));
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


}
