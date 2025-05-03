package edu.lcaitlyn.cloudfilestorage.DTO;

import edu.lcaitlyn.cloudfilestorage.enums.ResourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
public class ResourceResponseDTO {
    String path;
    String name;
    Long size;
    ResourceType resourceType;
}
