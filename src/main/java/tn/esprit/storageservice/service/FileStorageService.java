
package tn.esprit.storageservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import tn.esprit.storageservice.entity.FileMetadata;
import tn.esprit.storageservice.repository.FileMetadataRepository;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final FileMetadataRepository repo;
    private final S3Client s3Client;

    private static final String BUCKET_NAME = "my-test-bucket";

    public void uploadFile(MultipartFile file, String username, String storageType) throws IOException {
        String safeFilename = file.getOriginalFilename().replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
        String s3Key = username + "/" + safeFilename;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(s3Key)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        FileMetadata meta = FileMetadata.builder()
                .filename(safeFilename)
                .username(username)
                .storageType(storageType)
                .build();

        repo.save(meta);
    }

    public List<FileMetadata> getUserFiles(String username) {
        return repo.findByUsername(username);
    }

    public byte[] download(String username, String filename) throws IOException {
        String s3Key = username + "/" + filename;

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(s3Key)
                .build();

        ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getObjectRequest);
        return objectBytes.asByteArray();
    }

    public void delete(String username, String filename) {
        String s3Key = username + "/" + filename;

        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(s3Key)
                .build();

        s3Client.deleteObject(deleteObjectRequest);

        repo.deleteAll(
                repo.findByUsername(username).stream()
                        .filter(f -> f.getFilename().equals(filename))
                        .toList()
        );
    }
}
