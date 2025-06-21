package tn.esprit.storageservice.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserQuota {

    @Id
    private String userId;

    private long s3Used;     // en octets
    private long s3Limit;

    private long cephfsUsed;
    private long cephfsLimit;

    private long rbdUsed;
    private long rbdLimit;
}
