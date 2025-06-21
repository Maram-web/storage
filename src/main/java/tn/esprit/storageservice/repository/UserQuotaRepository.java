package tn.esprit.storageservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.storageservice.model.UserQuota;

public interface UserQuotaRepository extends JpaRepository<UserQuota, String> {
}
