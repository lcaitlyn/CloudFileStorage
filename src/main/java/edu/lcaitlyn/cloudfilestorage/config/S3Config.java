package edu.lcaitlyn.cloudfilestorage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

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


    @Bean
    public S3Client s3Client() {
        AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider
                .create(AwsBasicCredentials
                        .create(
                                accessKey, secretKey
                        ));

        return S3Client.builder()
                .credentialsProvider(credentialsProvider)
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .forcePathStyle(true)
                .build();
    }
}
