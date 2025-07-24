package studyMate.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiFeedbackResponse {
    // 요청 데이터 요약
    private StudySessionSummary sessionSummary;
    
    // AI 피드백
    private String feedback;
    private String suggestions;
    private String motivation;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudySessionSummary {
        // 기본 정보
        private int studyTimeSeconds;
        private int studyTimeMinutes;
        private int restTimeSeconds;
        private int restTimeMinutes;
        private String mode;
        private String summary;
        
        // 상세 정보
        private String studyTopic;
        private String studyGoal;
        private String difficulty;
        private String concentration;
        private String mood;
        private String interruptions;
        private String studyMethod;
        private String environment;
        private String energyLevel;
        private String stressLevel;
        
        // 사용자 통계
        private int userTotalStudyTimeMinutes;
    }
} 