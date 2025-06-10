package studyMate.controller;

import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import studyMate.dto.ApiResponse;
import studyMate.dto.pomodoro.PomodoroStartReqDto;
import studyMate.entity.User;
import studyMate.service.TimerService;

@AllArgsConstructor
@RestController
@RequestMapping("/api/timer")
public class PomodoroController {
    // 타이머기능은 Websocket을 쓰기로했음.
    // 백에서는 요청만, 실질적기능은 프론트에서.

    private final TimerService timerService;


    @PostMapping("/start")
    public ApiResponse<?> startTimer(@AuthenticationPrincipal User user,
                                   @RequestBody PomodoroStartReqDto dto) {
        timerService.startTimer(user, dto);
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
