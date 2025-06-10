package studyMate.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import studyMate.dto.ApiResponse;
import studyMate.dto.pomodoro.PomodoroStartReqDto;
import studyMate.entity.User;

@RestController
@RequestMapping("/api/timer")
public class PomodoroController {

    @PostMapping("/start")
    public ApiResponse<?> startTimer(@AuthenticationPrincipal User user,
                                   @RequestBody PomodoroStartReqDto dto) {
        return new ApiResponse<>(true, "Timer started for user: " + user.getNickname());
    }

    @PostMapping("/stop")
    public ApiResponse<?> stopTimer(@AuthenticationPrincipal User user) {
        return new ApiResponse<>(true, "Timer stopped for user: " + user.getNickname());
    }

    @GetMapping("/history")
    public ApiResponse<?> getTimerHistory(@AuthenticationPrincipal User user) {
        return new ApiResponse<>(true, "Timer history for user: " + user.getNickname());
    }
}
