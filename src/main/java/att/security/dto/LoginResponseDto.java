package att.security.dto;

import att.dto.UserProfileDto;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponseDto {

    private String token;
    private String tokenType;
    private long expiresIn;
    private UserProfileDto profile;
}
