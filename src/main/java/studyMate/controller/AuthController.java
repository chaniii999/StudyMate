package studyMate.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import studyMate.dto.ApiResponse;
import studyMate.dto.SignUpReqDto;
import studyMate.service.UserService;


@RestController
@RequestMapping("/api/auth")
public class AuthController {


    @Autowired
    private UserService userService;


    @PostMapping("/signup")
    public ResponseEntity<ApiResponse> signUp(@Valid @RequestBody SignUpReqDto signUpReqDto) {
        userService.registerUser(signUpReqDto);
        return ResponseEntity.ok(new ApiResponse(true, "User registered successfully!"));
    }
}