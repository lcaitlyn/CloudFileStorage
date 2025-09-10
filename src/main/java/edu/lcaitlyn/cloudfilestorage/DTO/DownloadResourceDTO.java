package edu.lcaitlyn.cloudfilestorage.DTO;

import edu.lcaitlyn.cloudfilestorage.enums.Type;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DownloadResourceDTO {
    String filename;
    String key;
    byte[] data;
    Type type;
}
