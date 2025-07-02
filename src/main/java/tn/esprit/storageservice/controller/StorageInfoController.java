package tn.esprit.storageservice.controller;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/storage")
public class StorageInfoController {

    @GetMapping("/users")
    public List<Map<String, Object>> getUsersStorageInfo() {
        // Simulé – normalement, tu utiliseras un service qui interroge Ceph Rook via RBD/CRDs
        List<Map<String, Object>> users = new ArrayList<>();

       
        users.add(Map.of("id", 3, "username", "carol", "storageUsed", "100MB", "price", "1€"));

        return users;
    }
}
