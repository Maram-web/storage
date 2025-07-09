package tn.esprit.storageservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.storageservice.service.QuotaService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/quota")
@RequiredArgsConstructor
public class QuotaController {

    private final QuotaService quotaService;

    @GetMapping("/upgrade-options")
    public List<String> getUpgradeOptions() {
        return List.of(
                "📦 Pack Basic – 100MB – 1€",
                "📦 Pack Pro – 1GB – 4€",
                "📦 Pack Premium – 10GB – 8€"
        );
    }

    @PostMapping("/init")
    public void initQuota(@RequestParam String username, @RequestParam String bucket) {
        quotaService.initializeQuota(username, bucket);
    }

    @GetMapping("/usage")
    public Map<String, Object> getUsage(@RequestParam String username, @RequestParam String bucket) {
        long used = quotaService.getUsedQuota(username, bucket);
        long remaining = quotaService.getRemainingQuota(username, bucket);
        long limit = quotaService.getQuotaLimit();

        return Map.of(
                "username", username,
                "bucket", bucket,
                "used", used,
                "remaining", remaining,
                "limit", limit
        );
    }
}
