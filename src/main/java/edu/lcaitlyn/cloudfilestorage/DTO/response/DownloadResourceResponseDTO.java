package edu.lcaitlyn.cloudfilestorage.DTO.response;

import edu.lcaitlyn.cloudfilestorage.enums.ResourceType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DownloadResourceResponseDTO {
    String filename;
    byte [] data;
    ResourceType resourceType;
}
