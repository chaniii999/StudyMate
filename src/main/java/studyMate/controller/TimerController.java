package studyMate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import studyMate.dto.ApiResponse;
import studyMate.entity.User;
import studyMate.service.TimerService;
import studyMate.entity.Timer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.http.ResponseEntity;
import lombok.Getter;
import lombok.Setter;
import java.util.List;
import studyMate.repository.TimerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/timer")
@RequiredArgsConstructor
public class TimerController {
    
    private final TimerService timerService;
    private final TimerRepository timerRepository;
    private static final Logger log = LoggerFactory.getLogger(TimerController.class);

    // 타이머 기록 저장
    @PostMapping("/save")
    public ResponseEntity<ApiResponse<Timer>> saveTimerRecord(
            @AuthenticationPrincipal User user,
            @RequestBody TimerSaveRequest request
    ) {
        if (user == null) {
            log.warn("[TimerController] 인증되지 않은 사용자가 기록 저장 시도: {}", request);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>(false, "로그인이 필요합니다.", null));
        }
        Timer timer = timerService.saveTimerRecord(
                user,
                request.getStudyMinutes(),
                request.getRestMinutes(),
                request.getStartTime(),
                request.getEndTime(),
                request.getMode(),
                request.getSummary()
        );
        return ResponseEntity.ok(new ApiResponse<>(true, "타이머 기록 저장 완료", timer));
    }

    // 타이머 기록 저장용 DTO
    @Getter @Setter
    public static class TimerSaveRequest {
        private int studyMinutes;
        private int restMinutes;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String mode;
        private String summary;
    }

    // 타이머 기록 조회
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<Timer>>> getTimerHistory(@AuthenticationPrincipal User user) {
        List<Timer> history = timerRepository.findByUserOrderByStartTimeDesc(user);
        return ResponseEntity.ok(new ApiResponse<>(true, "타이머 기록 조회 성공", history));
    }

//    @GetMapping("/statistics")
//    public ApiResponse<?> getTimerStatistics(@AuthenticationPrincipal User user) {
//        return new ApiResponse<>(true, "Timer statistics for user: " + user.getNickname(),
//            timerService.getTimerStatistics(user));
//    }
}
