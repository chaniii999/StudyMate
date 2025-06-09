package studyMate.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import studyMate.dto.ApiResponse;
import studyMate.dto.SignInReq;
import studyMate.dto.TokenDto;
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
    public ApiResponse<?> signIn(@Valid @RequestBody SignInReq signInReq) {
        TokenDto tokenDto = userService.login(signInReq);
        return new ApiResponse<>(true, "User registered successfully!", tokenDto);
    }

    @PostMapping("/sign-up")
    public ApiResponse<?> signUp(@Valid @RequestBody SignUpReqDto signUpReqDto) {
        userService.registerUser(signUpReqDto);
        return new ApiResponse<>(true, "User registered successfully!");
    }

    @PostMapping("/send-code")
    public ApiResponse<?> sendCode(@RequestBody SendCodeDto dto) {
        authService.sendCode(dto.getEmail());
        return new ApiResponse<>(true, "Verification code sent to " + dto.getEmail());
    }

    @PostMapping("/verify-code")
    public ApiResponse<?> verifyCode(@RequestBody VerifyCodeDto dto) {
        authService.verifyCode(dto.getEmail(), dto.getCode());
        return new ApiResponse<>(true, "Verification successful!");
    }
}