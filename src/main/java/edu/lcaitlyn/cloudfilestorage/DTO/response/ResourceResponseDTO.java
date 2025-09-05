package edu.lcaitlyn.cloudfilestorage.DTO.response;

import edu.lcaitlyn.cloudfilestorage.enums.Type;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResourceResponseDTO {
    String path;
    String name;
    Long size;
    Type type;
}
