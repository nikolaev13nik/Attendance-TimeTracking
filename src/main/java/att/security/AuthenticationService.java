package att.security;

import att.security.dto.LoginRequestDto;
import att.security.dto.LoginResponseDto;

public interface AuthenticationService {

    LoginResponseDto authenticate(LoginRequestDto request);
}
