package edu.lcaitlyn.cloudfilestorage.DTO;

import edu.lcaitlyn.cloudfilestorage.enums.Type;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StorageDTO {
    private final String key;
    private final Type type;
    private final long size;
}
