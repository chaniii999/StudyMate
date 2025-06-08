package studyMate.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class VerifyCodeDto {
    private String email;
    private String code;


}
