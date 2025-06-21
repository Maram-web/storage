package tn.esprit.storageservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/quota")
public class QuotaController {

    @GetMapping("/upgrade-options")
    public List<String> getUpgradeOptions() {
        return List.of(
                "📦 Pack Basic – 100MB – 1€",
                "📦 Pack Pro – 1GB – 4€",
                "📦 Pack Premium – 10GB – 8€"
        );
    }
}
