package edu.lcaitlyn.cloudfilestorage.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Slf4j
@Configuration
public class S3Config {

    @Value("${aws.accessKeyId}")
    private String accessKey;

    @Value("${aws.secretKey}")
    private String secretKey;

    @Value("${aws.region}")
    private String region;

    @Value("${aws.s3.endpoint}")
    private String endpoint;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    private S3Client createS3Bucket(S3Client s3Client) {
        try {
            boolean found = s3Client.listBuckets().buckets().stream().anyMatch(
                    b -> b.name().equals(bucketName));

            if (!found) {
                s3Client.createBucket(b -> b.bucket(bucketName));
                log.info("S3Config: created bucket: " + bucketName);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return s3Client;
    }

    @Bean
    public S3Client s3Client() {
        AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider
                .create(AwsBasicCredentials
                        .create(
                                accessKey, secretKey
                        ));

        S3Client s3Client = S3Client.builder()
                .credentialsProvider(credentialsProvider)
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .forcePathStyle(true)
                .build();

        return createS3Bucket(s3Client);
    }
}
