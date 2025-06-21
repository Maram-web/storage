package tn.esprit.storageservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.storageservice.entity.FileMetadata;

import java.util.List;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {
    List<FileMetadata> findByUsername(String username);
}
