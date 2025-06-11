package edu.lcaitlyn.cloudfilestorage.DTO.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRequestDTO {
    @Size(min = 5, max = 30, message = "Login length should be >=5 and <= 30")
    String username;
    @Size(min = 5, max = 30, message = "Password length should be >=5 and <= 30")
    String password;
}
