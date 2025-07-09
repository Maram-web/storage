package tn.esprit.storageservice.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class QuotaService {

    private final Map<String, Map<String, Long>> userBucketUsage = new HashMap<>();
    private final long MAX_SIZE = 2 * 1024 * 1024; // 2 MB pour test

    public boolean canUpload(String username, String bucketName, long fileSize) {
        userBucketUsage.putIfAbsent(username, new HashMap<>());
        Map<String, Long> bucketUsage = userBucketUsage.get(username);

        long used = bucketUsage.getOrDefault(bucketName, 0L);
        return (used + fileSize) <= MAX_SIZE;
    }

    public void updateUsage(String username, String bucketName, long fileSize) {
        userBucketUsage.putIfAbsent(username, new HashMap<>());
        Map<String, Long> bucketUsage = userBucketUsage.get(username);

        long newUsage = bucketUsage.getOrDefault(bucketName, 0L) + fileSize;
        bucketUsage.put(bucketName, newUsage);
    }

    public long getUsedQuota(String username, String bucketName) {
        return userBucketUsage
                .getOrDefault(username, new HashMap<>())
                .getOrDefault(bucketName, 0L);
    }

    public long getRemainingQuota(String username, String bucketName) {
        return MAX_SIZE - getUsedQuota(username, bucketName);
    }

    public String suggestUpgrade() {
        return "💡 Votre quota est atteint. Voir les packs : /api/quota/upgrade-options";
    }

    public void initializeQuota(String username, String bucketName) {
        userBucketUsage.putIfAbsent(username, new HashMap<>());
        userBucketUsage.get(username).put(bucketName, 0L); // init à 0
    }

    public long getQuotaLimit() {
        return MAX_SIZE;
    }
}
