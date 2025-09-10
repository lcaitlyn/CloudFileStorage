package edu.lcaitlyn.cloudfilestorage.DTO;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResourceMetadata {
    private final Long contentLength;
    private final String contentType;
    private final byte[] data;
}
