package edu.lcaitlyn.cloudfilestorage.DTO.request;

import edu.lcaitlyn.cloudfilestorage.models.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MoveResourceRequestDTO {
    User user;
    String from;
    String to;
}
