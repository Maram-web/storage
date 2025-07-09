package tn.esprit.storageservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import tn.esprit.storageservice.service.QuotaService;

import java.util.List;
import java.util.stream.Collectors;

// BucketController.java
@RestController
@RequestMapping("/api/buckets")
@RequiredArgsConstructor
public class BucketController {

    private final QuotaService quotaService; // ou bucketService si tu as un service dédié
    private final S3Client s3Client;



    @GetMapping
    public ResponseEntity<List<String>> listBuckets(Authentication auth) {
        String username = auth.getName();
        ListBucketsResponse response = s3Client.listBuckets();

        // Filtrer les buckets appartenant à l’utilisateur
        List<String> userBuckets = response.buckets().stream()
                .map(Bucket::name)
                .filter(name -> name.startsWith(username + "-")) // nommage buckets = username-nom
                .collect(Collectors.toList());

        return ResponseEntity.ok(userBuckets);
    }
}
