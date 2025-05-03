package edu.lcaitlyn.cloudfilestorage.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class S3Properties {
    @Value("${aws.s3.bucket}")
    String bucket;
}
