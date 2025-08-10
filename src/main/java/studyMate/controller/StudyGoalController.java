package studyMate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import studyMate.dto.ApiResponse;
import studyMate.dto.studygoal.StudyGoalRequest;
import studyMate.dto.studygoal.StudyGoalResponse;
import studyMate.dto.studygoal.StudyGoalStatistics;
import studyMate.entity.User;
import studyMate.service.StudyGoalService;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/study-goals")
@RequiredArgsConstructor
public class StudyGoalController {
    
    private final StudyGoalService studyGoalService;
    
    /**
     * 사용자의 모든 학습목표 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<StudyGoalResponse>>> getAllStudyGoals(
            @AuthenticationPrincipal User user) {
        
        List<StudyGoalResponse> studyGoals = studyGoalService.getAllStudyGoals(user);
        return ResponseEntity.ok(new ApiResponse<>(true, "학습목표 조회 성공", studyGoals));
    }
    
    /**
     * 사용자의 활성 학습목표 조회
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<StudyGoalResponse>>> getActiveStudyGoals(
            @AuthenticationPrincipal User user) {
        
        List<StudyGoalResponse> activeGoals = studyGoalService.getActiveStudyGoals(user);
        return ResponseEntity.ok(new ApiResponse<>(true, "활성 학습목표 조회 성공", activeGoals));
    }
    
    /**
     * 특정 학습목표 조회
     */
    @GetMapping("/{goalId}")
    public ResponseEntity<ApiResponse<StudyGoalResponse>> getStudyGoal(
            @AuthenticationPrincipal User user,
            @PathVariable Long goalId) {
        
        StudyGoalResponse studyGoal = studyGoalService.getStudyGoal(user, goalId);
        return ResponseEntity.ok(new ApiResponse<>(true, "학습목표 조회 성공", studyGoal));
    }
    
    /**
     * 새로운 학습목표 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<StudyGoalResponse>> createStudyGoal(
            @AuthenticationPrincipal User user,
            @RequestBody StudyGoalRequest request) {
        
        log.info("학습목표 생성 요청: {} (사용자: {})", request.getTitle(), user.getEmail());
        StudyGoalResponse createdGoal = studyGoalService.createStudyGoal(user, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "학습목표 생성 성공", createdGoal));
    }
    
    /**
     * 학습목표 수정
     */
    @PutMapping("/{goalId}")
    public ResponseEntity<ApiResponse<StudyGoalResponse>> updateStudyGoal(
            @AuthenticationPrincipal User user,
            @PathVariable Long goalId,
            @RequestBody StudyGoalRequest request) {
        
        log.info("학습목표 수정 요청: {} (사용자: {})", goalId, user.getEmail());
        StudyGoalResponse updatedGoal = studyGoalService.updateStudyGoal(user, goalId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "학습목표 수정 성공", updatedGoal));
    }
    
    /**
     * 학습목표 삭제
     */
    @DeleteMapping("/{goalId}")
    public ResponseEntity<ApiResponse<Void>> deleteStudyGoal(
            @AuthenticationPrincipal User user,
            @PathVariable Long goalId) {
        
        log.info("학습목표 삭제 요청: {} (사용자: {})", goalId, user.getEmail());
        studyGoalService.deleteStudyGoal(user, goalId);
        return ResponseEntity.ok(new ApiResponse<>(true, "학습목표 삭제 성공", null));
    }
    
    /**
     * 학습목표별 통계 조회
     */
    @GetMapping("/{goalId}/statistics")
    public ResponseEntity<ApiResponse<StudyGoalStatistics>> getStudyGoalStatistics(
            @AuthenticationPrincipal User user,
            @PathVariable Long goalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        // 기본값 설정 (최근 30일)
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        log.info("학습목표 통계 조회: {} (기간: {} ~ {})", goalId, startDate, endDate);
        StudyGoalStatistics statistics = studyGoalService.getStudyGoalStatistics(user, goalId, startDate, endDate);
        return ResponseEntity.ok(new ApiResponse<>(true, "학습목표 통계 조회 성공", statistics));
    }
}