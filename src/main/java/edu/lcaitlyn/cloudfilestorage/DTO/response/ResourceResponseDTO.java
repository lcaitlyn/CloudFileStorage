package edu.lcaitlyn.cloudfilestorage.DTO.response;

import edu.lcaitlyn.cloudfilestorage.enums.ResourceType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResourceResponseDTO {
    String path;
    String name;
    Long size;
    ResourceType resourceType;
}
