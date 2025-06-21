package tn.esprit.storageservice.service;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class QuotaService {

    private final Map<String, Long> userQuota = new HashMap<>();
    private final long MAX_SIZE = 2 * 1024 * 1024; // 50 MB mais 2  pour le test

    public boolean canUpload(String username, long fileSize) {
        long used = userQuota.getOrDefault(username, 0L);
        return (used + fileSize) <= MAX_SIZE;
    }

    public void updateUsage(String username, long fileSize) {
        userQuota.put(username, userQuota.getOrDefault(username, 0L) + fileSize);
    }

    public long getRemainingQuota(String username) {
        return MAX_SIZE - userQuota.getOrDefault(username, 0L);
    }

    public String suggestUpgrade() {
        return "💡 Votre quota est atteint. Voir les packs : /api/storage/quota/upgrade-options";
    }
}
