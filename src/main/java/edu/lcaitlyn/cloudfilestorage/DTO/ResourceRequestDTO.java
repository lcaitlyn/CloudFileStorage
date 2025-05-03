package edu.lcaitlyn.cloudfilestorage.DTO;

import edu.lcaitlyn.cloudfilestorage.models.User;
import lombok.Builder;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
public class ResourceRequestDTO {
    User user;
    MultipartFile [] files;
    String path;
}
