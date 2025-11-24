package studyMate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import studyMate.dto.ApiResponse;
import studyMate.entity.Timer;
import studyMate.entity.User;
import studyMate.service.TimerService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/timer")
@RequiredArgsConstructor
public class TimerController {
    
    private final TimerService timerService;
    
    /**
     * 타이머 기록 조회
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<Timer>>> getTimerHistory(
            @AuthenticationPrincipal User user) {
        
        List<Timer> history = timerService.getTimerHistory(user);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
    
    /**
     * 기간별 타이머 기록 조회
     */
    @GetMapping("/history/range")
    public ResponseEntity<ApiResponse<List<Timer>>> getTimerHistoryByRange(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        List<Timer> history = timerService.getTimerHistoryByDateRange(user, startTime, endTime);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
    
    /**
     * 학습목표별 타이머 기록 조회
     */
    @GetMapping("/history/goal/{studyGoalId}")
    public ResponseEntity<ApiResponse<List<Timer>>> getTimerHistoryByStudyGoal(
            @AuthenticationPrincipal User user,
            @PathVariable Long studyGoalId) {
        List<Timer> history = timerService.getTimerHistoryByStudyGoal(user, studyGoalId);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
    
    /**
     * 타이머 기록 저장
     */
    @PostMapping("/save")
    public ResponseEntity<ApiResponse<Timer>> saveTimerRecord(
            @AuthenticationPrincipal User user,
            @RequestBody Timer timer,
            @RequestParam(required = false) Long studyGoalId) {
        
        log.info("타이머 기록 저장 요청: 학습시간 {}분, 학습목표 ID: {}", 
                timer.getStudyTime() / 60, studyGoalId);
        
        Timer savedTimer = timerService.saveTimerRecord(user, timer, studyGoalId);
        return ResponseEntity.ok(ApiResponse.success(savedTimer));
    }
    
    /**
     * 타이머 기록 삭제
     */
    @DeleteMapping("/{timerId}")
    public ResponseEntity<ApiResponse<Void>> deleteTimerRecord(
            @AuthenticationPrincipal User user,
            @PathVariable Long timerId) {
        
        try {
            boolean deleted = timerService.deleteTimerRecord(user, timerId);
            if (deleted) {
                return ResponseEntity.ok(ApiResponse.success(null));
            } else {
                // 타이머가 존재하지 않거나 권한이 없는 경우
                return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("타이머 기록을 찾을 수 없거나 삭제할 권한이 없습니다."));
            }
        } catch (Exception e) {
            log.error("타이머 기록 삭제 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("타이머 기록 삭제에 실패했습니다: " + e.getMessage()));
        }
    }
    
    /**
     * 홈 화면용 통계 조회
     */
    @GetMapping("/home-stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHomeStats(
            @AuthenticationPrincipal User user) {
        
        int todayStudyMinutes = timerService.getTodayStudyTime(user);
        int weekStudyMinutes = timerService.getWeekStudyTime(user);
        
        Map<String, Object> stats = Map.of(
                "todayStudyMinutes", todayStudyMinutes,
                "weekStudyMinutes", weekStudyMinutes,
                "todayStudySeconds", todayStudyMinutes * 60,
                "weekStudySeconds", weekStudyMinutes * 60
        );
        
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
    
    /**
     * 전체 통계 조회
     */
    @GetMapping("/total-stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTotalStats(
            @AuthenticationPrincipal User user) {
        
        int totalStudyMinutes = timerService.getTotalStudyTime(user);
        int totalSessions = timerService.getTotalSessionCount(user);
        double averageSessionTime = timerService.getAverageSessionTime(user);
        int longestSessionTime = timerService.getLongestSessionTime(user);
        
        Map<String, Object> stats = Map.of(
                "totalStudyMinutes", totalStudyMinutes,
                "totalStudyHours", totalStudyMinutes / 60,
                "totalSessions", totalSessions,
                "averageSessionMinutes", Math.round(averageSessionTime * 100.0) / 100.0,
                "longestSessionMinutes", longestSessionTime
        );
        
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
    
    /**
     * 학습목표별 통계 조회
     */
    @GetMapping("/goal-stats/{studyGoalId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGoalStats(
            @AuthenticationPrincipal User user,
            @PathVariable Long studyGoalId) {
        
        int goalStudyMinutes = timerService.getStudyTimeByGoal(user, studyGoalId);
        int goalSessions = timerService.getSessionCountByGoal(user, studyGoalId);
        
        Map<String, Object> stats = Map.of(
                "studyGoalId", studyGoalId,
                "goalStudyMinutes", goalStudyMinutes,
                "goalStudyHours", goalStudyMinutes / 60,
                "goalSessions", goalSessions,
                "averageSessionMinutes", goalSessions > 0 ? 
                        Math.round((double) goalStudyMinutes / goalSessions * 100.0) / 100.0 : 0.0
        );
        
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
    
    /**
     * 월별 통계 조회
     */
    @GetMapping("/month-stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMonthStats(
            @AuthenticationPrincipal User user,
            @RequestParam int year,
            @RequestParam int month) {
        
        int monthStudyMinutes = timerService.getMonthStudyTime(user, year, month);
        
        Map<String, Object> stats = Map.of(
                "year", year,
                "month", month,
                "monthStudyMinutes", monthStudyMinutes,
                "monthStudyHours", monthStudyMinutes / 60
        );
        
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
    
    /**
     * 연도별 통계 조회
     */
    @GetMapping("/year-stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getYearStats(
            @AuthenticationPrincipal User user,
            @RequestParam int year) {
        
        int yearStudyMinutes = timerService.getYearStudyTime(user, year);
        
        Map<String, Object> stats = Map.of(
                "year", year,
                "yearStudyMinutes", yearStudyMinutes,
                "yearStudyHours", yearStudyMinutes / 60
        );
        
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}