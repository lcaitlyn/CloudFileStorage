package edu.lcaitlyn.cloudfilestorage.DTO;

import edu.lcaitlyn.cloudfilestorage.models.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
public class MoveResourceRequestDTO {
    User user;
    String from;
    String to;
}
