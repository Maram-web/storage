package tn.esprit.storageservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/storage/test")
public class TestController {

    @GetMapping("/public")
    public String publicEndpoint() {
        return "Accessible sans token";
    }

    @GetMapping("/secure")
    public String securedEndpoint() {
        return "Accessible avec JWT ✅";
    }
}
