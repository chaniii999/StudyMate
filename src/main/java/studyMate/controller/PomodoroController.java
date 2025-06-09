package studyMate.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import studyMate.dto.ApiResponse;
import studyMate.dto.pomodoro.PomodoroStartReqDto;
import studyMate.entity.User;

import java.nio.file.attribute.UserPrincipal;

@RestController
@RequestMapping("/api/timer")
public class PomodoroController {

    @PostMapping("/start")
    public ApiResponse<?> startTimer(@AuthenticationPrincipal UserPrincipal user,
                                     @RequestBody PomodoroStartReqDto dto) {


        return new ApiResponse<>(true, "Timer started");
    }

    @PostMapping("/start")
    public ApiResponse<?> stopTimer(@RequestBody User user) {


        return new ApiResponse<>(true, "Timer stopped for user: " + user.getNickname());
    }

    @GetMapping("/history")
    public ApiResponse<?> history(@RequestParam String userId) {
        // 여기에 사용자 ID에 대한 타이머 기록을 조회하는 로직을 추가합니다.
        // 예시로 단순히 성공 메시지를 반환합니다.
        return new ApiResponse<>(true, "Timer history for user ID: " + userId);
    }

}
