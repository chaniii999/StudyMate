package studyMate.dto.studygoal;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyGoalStatistics {
    
    private Long goalId;
    private String title;
    private String subject;
    private String color;
    
    // 전체 통계
    private Integer totalStudyHours;
    private Integer totalStudySessions;
    private Double averageSessionTime;
    private Double progressRate;
    
    // 기간별 통계
    private Integer studyHoursInPeriod;
    private Integer studySessionsInPeriod;
    
    // 일별 통계 (차트용)
    private List<DailyStudyData> dailyData;
    
    // 주간별 통계
    private Map<String, Integer> weeklyData;
    
    // 월별 통계
    private Map<String, Integer> monthlyData;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyStudyData {
        private LocalDate date;
        private Integer studyMinutes;
        private Integer sessions;
        private String dayOfWeek;
    }
}