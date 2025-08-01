package studyMate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import studyMate.dto.ApiResponse;
import studyMate.entity.User;
import studyMate.service.TimerService;
import studyMate.entity.Timer;
import java.time.LocalDateTime;

import org.springframework.http.ResponseEntity;
import lombok.Getter;
import lombok.Setter;
import java.util.List;
import studyMate.repository.TimerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import java.time.LocalDate;
import java.util.Map;
import java.util.HashMap;

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
        
        // 실제 경과 시간 계산 (초 단위)
        int actualStudySeconds = request.getStudyTimes(); // 초 단위
        int actualRestSeconds = request.getRestTimes();   // 초 단위
        
        // 클라이언트에서 정확한 시간을 제공한 경우 우선 사용
        if (request.getActualStudySeconds() != null && request.getActualRestSeconds() != null) {
            actualStudySeconds = request.getActualStudySeconds().intValue();
            actualRestSeconds = request.getActualRestSeconds().intValue();
            log.info("클라이언트에서 제공한 정확한 시간 - 학습: {}초({}분), 휴식: {}초({}분)", 
                    actualStudySeconds, actualStudySeconds/60, 
                    actualRestSeconds, actualRestSeconds/60);
        }
        // startTime과 endTime이 제공된 경우 실제 경과 시간 계산
        else if (request.getStartTime() != null && request.getEndTime() != null) {
            long totalSeconds = java.time.Duration.between(request.getStartTime(), request.getEndTime()).getSeconds();
            
            if ("STUDY".equals(request.getMode()) || request.getMode() == null) {
                actualStudySeconds = (int) totalSeconds;
                actualRestSeconds = 0;
            } else if ("BREAK".equals(request.getMode())) {
                actualStudySeconds = 0;
                actualRestSeconds = (int) totalSeconds;
            }
            // 포모도로 모드 등에서는 클라이언트에서 전달한 값을 사용
        }
        
        log.info("타이머 기록 저장 요청 - 사용자: {}, 실제 학습시간: {}초({}분), 실제 휴식시간: {}초({}분), 모드: {}", 
                user.getNickname(), actualStudySeconds, actualStudySeconds/60, actualRestSeconds, actualRestSeconds/60, request.getMode());
        
        Timer timer = timerService.saveTimerRecord(
                user,
                actualStudySeconds, // 초 단위로 직접 전달
                actualRestSeconds,  // 초 단위로 직접 전달
                request.getStartTime(),
                request.getEndTime(),
                request.getMode(),
                request.getSummary()
        );
        return ResponseEntity.ok(new ApiResponse<>(true, "타이머 기록 저장 완료", timer));
    }

    // 홈 통계 조회 API
    @GetMapping("/home-stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHomeStats(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>(false, "로그인이 필요합니다.", null));
        }
        
        try {
            Map<String, Object> stats = timerService.getHomeStats(user);
            return ResponseEntity.ok(new ApiResponse<>(true, "홈 통계 조회 성공", stats));
        } catch (Exception e) {
            log.error("홈 통계 조회 에러 - 사용자: {}", user.getNickname(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "홈 통계 조회에 실패했습니다.", null));
        }
    }

    // 테스트용 API (인증 없음)
    @GetMapping("/test-stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTestStats() {
        log.info("테스트 홈 통계 API 호출됨");
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("todayStudyMinutes", 45);
            stats.put("weekStudyMinutes", 180);
            stats.put("todayStudySeconds", 2700);
            stats.put("weekStudySeconds", 10800);
            
            log.info("테스트 홈 통계 응답: {}", stats);
            return ResponseEntity.ok(new ApiResponse<>(true, "테스트 홈 통계 조회 성공", stats));
        } catch (Exception e) {
            log.error("테스트 홈 통계 조회 에러", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "테스트 홈 통계 조회에 실패했습니다.", null));
        }
    }

    // 타이머 기록 저장용 DTO
    @Getter @Setter
    public static class TimerSaveRequest {
        private int studyTimes;          // 학습 시간 (초 단위)
        private int restTimes;           // 휴식 시간 (초 단위)
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String mode;
        private String summary;
        
        // 추가 정보 (선택적)
        private Long actualStudySeconds; // 실제 학습 시간 (초) - 우선순위 높음
        private Long actualRestSeconds;  // 실제 휴식 시간 (초) - 우선순위 높음
        private String studyTopic;       // 학습 주제
        private String studyGoal;        // 학습 목표
    }

    // 타이머 기록 조회
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<Timer>>> getTimerHistory(@AuthenticationPrincipal User user) {
        List<Timer> history = timerRepository.findByUserOrderByStartTimeDesc(user);
        return ResponseEntity.ok(new ApiResponse<>(true, "타이머 기록 조회 성공", history));
    }

    // 타이머 기록 삭제
    @DeleteMapping("/{timerId}")
    public ResponseEntity<ApiResponse<Void>> deleteTimerRecord(
            @AuthenticationPrincipal User user,
            @PathVariable Long timerId
    ) {
        if (user == null) {
            log.warn("[TimerController] 인증되지 않은 사용자가 기록 삭제 시도: timerId={}", timerId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>(false, "로그인이 필요합니다.", null));
        }
        
        try {
            boolean deleted = timerService.deleteTimerRecord(user, timerId);
            if (deleted) {
                log.info("[TimerController] 타이머 기록 삭제 성공 - 사용자: {}, timerId: {}", user.getNickname(), timerId);
                return ResponseEntity.ok(new ApiResponse<>(true, "타이머 기록이 삭제되었습니다.", null));
            } else {
                log.warn("[TimerController] 타이머 기록 삭제 실패 - 권한 없음 또는 존재하지 않음: 사용자={}, timerId={}", user.getNickname(), timerId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, "삭제 권한이 없거나 존재하지 않는 기록입니다.", null));
            }
        } catch (Exception e) {
            log.error("[TimerController] 타이머 기록 삭제 에러 - 사용자: {}, timerId: {}", user.getNickname(), timerId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(false, "타이머 기록 삭제에 실패했습니다.", null));
        }
    }

//    @GetMapping("/statistics")
//    public ApiResponse<?> getTimerStatistics(@AuthenticationPrincipal User user) {
//        return new ApiResponse<>(true, "Timer statistics for user: " + user.getNickname(),
//            timerService.getTimerStatistics(user));
//    }
}
