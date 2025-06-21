package tn.esprit.storageservice;

import org.springframework.beans.factory.annotation.Value; // CORRECT import
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration // NE PAS mettre "@Configuration" coupé
public class CephS3Config {

    @Value("${ceph.s3.endpoint}")
    private String endpoint;

    @Value("${ceph.s3.region}")
    private String region;

    @Value("${ceph.s3.accessKey}")  // OK
    private String accessKey;

    @Value("${ceph.s3.secretKey}")  // OK
    private String secretKey;


    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .forcePathStyle(true) // obligatoire pour Ceph
                .build();
    }
}
