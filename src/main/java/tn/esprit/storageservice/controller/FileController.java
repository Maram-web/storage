package tn.esprit.storageservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.storageservice.entity.FileMetadata;
import tn.esprit.storageservice.service.FileStorageService;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService storageService;

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file,
                                         @RequestParam("type") String type,
                                         Authentication auth) throws IOException {
        System.out.println("🔥 Upload endpoint hit: " + file.getOriginalFilename() + " by " + auth.getName());
        storageService.uploadFile(file, auth.getName(), type);
        return ResponseEntity.ok("Uploaded ✅");

    }

    @GetMapping
    public ResponseEntity<List<FileMetadata>> list(Authentication auth) {
        return ResponseEntity.ok(storageService.getUserFiles(auth.getName()));
    }

    @GetMapping("/{filename}")
    public ResponseEntity<byte[]> download(@PathVariable String filename,
                                           Authentication auth) throws IOException {
        byte[] data = storageService.download(auth.getName(), filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    @DeleteMapping("/{filename}")
    public ResponseEntity<String> delete(@PathVariable String filename,
                                         Authentication auth) throws IOException {
        storageService.delete(auth.getName(), filename);
        return ResponseEntity.ok("Deleted ✅");
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("✅ API is reachable");
    }

}