package studyMate.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import studyMate.dto.ApiResponse;
import studyMate.dto.SignUpReqDto;
import studyMate.service.AuthService;
import studyMate.service.UserService;


@AllArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private UserService userService;
    private AuthService authService;


    @PostMapping("/signup")
    public ResponseEntity<ApiResponse> signUp(@Valid @RequestBody SignUpReqDto signUpReqDto) {
        userService.registerUser(signUpReqDto);
        return ResponseEntity.ok(new ApiResponse(true, "User registered successfully!"));
    }

    @PostMapping("/send-code")
    public ResponseEntity<ApiResponse> sendCode(@RequestBody String email) {
        authService.sendCode(email);
        return ResponseEntity.ok(new ApiResponse(true, "Verification code sent to " + email));
    }

    @PostMapping("/verify-code")
    public ResponseEntity<ApiResponse> verifyCode(@RequestBody String email, @RequestBody String code) {
        try {
            authService.verifyCode(email, code);
            return ResponseEntity.ok(new ApiResponse(true, "Verification successful!"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

}