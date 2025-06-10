package studyMate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import studyMate.dto.ApiResponse;
import studyMate.entity.User;
import studyMate.service.TimerService;

@RestController
@RequestMapping("/api/timer")
@RequiredArgsConstructor
public class TimerController {
    
    private final TimerService timerService;

//    @GetMapping("/history")
//    public ApiResponse<?> getTimerHistory(@AuthenticationPrincipal User user) {
//        return new ApiResponse<>(true, "Timer history for user: " + user.getNickname(),
//            timerService.getTimerHistory(user));
//    }
//
//    @GetMapping("/statistics")
//    public ApiResponse<?> getTimerStatistics(@AuthenticationPrincipal User user) {
//        return new ApiResponse<>(true, "Timer statistics for user: " + user.getNickname(),
//            timerService.getTimerStatistics(user));
//    }
}
