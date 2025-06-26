package studyMate.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studyMate.dto.ApiResponse;
import studyMate.dto.auth.SignInReq;
import studyMate.dto.TokenDto;
import studyMate.dto.auth.LoginResponseDto;
import studyMate.dto.auth.RefreshTokenRequest;
import studyMate.dto.auth.SendCodeDto;
import studyMate.dto.auth.SignUpReqDto;
import studyMate.dto.auth.VerifyCodeDto;
import studyMate.service.AuthService;
import studyMate.service.UserService;

@AllArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private UserService userService;
    private AuthService authService;

    @PostMapping("/sign-in")
    public ApiResponse<LoginResponseDto> signIn(@Valid @RequestBody SignInReq signInReq) {
        LoginResponseDto loginResponse = userService.login(signInReq);
        return new ApiResponse<>(true, "로그인이 성공적으로 완료되었습니다.", loginResponse);
    }

    @PostMapping("/sign-up")
    public ApiResponse<?> signUp(@Valid @RequestBody SignUpReqDto signUpReqDto) {
        userService.registerUser(signUpReqDto);
        return new ApiResponse<>(true, "회원가입이 성공적으로 완료되었습니다!");
    }

    @PostMapping("/send-code")
    public ApiResponse<?> sendCode(@RequestBody SendCodeDto dto) {
        authService.sendCode(dto.getEmail());
        return new ApiResponse<>(true, "인증 코드가 " + dto.getEmail() + "로 전송되었습니다.");
    }

    @PostMapping("/verify-code")
    public ApiResponse<?> verifyCode(@RequestBody VerifyCodeDto dto) {
        authService.verifyCode(dto.getEmail(), dto.getCode());
        return new ApiResponse<>(true, "이메일 인증이 성공적으로 완료되었습니다!");
    }

    @PostMapping("/refresh")
    public ApiResponse<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        TokenDto tokenDto = authService.refreshToken(request.getRefreshToken());
        return new ApiResponse<>(true, "토큰이 성공적으로 갱신되었습니다.", tokenDto);
    }
}