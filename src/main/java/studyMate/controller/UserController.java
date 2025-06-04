package studyMate.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import studyMate.dto.SignUpReqDto;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignUpReqDto requestDto) {
        // 유효성 검사를 통과하면 서비스로 전달
        return ResponseEntity.ok("회원가입 성공");
    }
}
