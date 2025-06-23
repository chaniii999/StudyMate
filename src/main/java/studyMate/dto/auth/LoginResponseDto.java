package studyMate.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import studyMate.dto.TokenDto;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDto {
    private String userId;
    private String email;
    private String nickname;
    private Integer age;
    private String sex;
    private TokenDto token;
    private String message;
} 