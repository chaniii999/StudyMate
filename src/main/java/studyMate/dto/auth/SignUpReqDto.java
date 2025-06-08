package studyMate.dto.auth;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import studyMate.entity.User;

@Builder
@Getter @Setter
@ToString
public class SignUpReqDto {

    @NotBlank
    @Email
    @Size(max = 40)
    private String email;

    @NotBlank
    @Size(min = 8, max = 40)
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]*$", message = "비밀번호는 영문자와 숫자를 포함해야 합니다.")
    private String password;

    @NotBlank
    @Size(max = 15)
    private String nickname;

    @Min(1)
    @Max(100)
    private Integer age;

    @NotNull
    @Pattern(regexp = "남|여", message = "성별은 '남' 또는 '여' 중 하나여야 합니다.")
    private User.Sex sex;
}
