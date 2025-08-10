package studyMate.dto.studygoal;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import studyMate.entity.GoalStatus;
import studyMate.entity.StudyGoal;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyGoalResponse {
    
    private Long id;
    private String title;
    private String subject;
    private String description;
    private String color;
    private LocalDate startDate;
    private LocalDate targetDate;
    private Integer targetHours;
    private Integer targetSessions;
    private GoalStatus status;
    private Integer currentHours;
    private Integer currentMinutes;
    private Integer currentSessions;
    private Double progressRate;
    private Integer remainingHours;
    private Integer remainingMinutes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static StudyGoalResponse from(StudyGoal studyGoal) {
        return StudyGoalResponse.builder()
                .id(studyGoal.getId())
                .title(studyGoal.getTitle())
                .subject(studyGoal.getSubject())
                .description(studyGoal.getDescription())
                .color(studyGoal.getColor())
                .startDate(studyGoal.getStartDate())
                .targetDate(studyGoal.getTargetDate())
                .targetHours(studyGoal.getTargetHours())
                .targetSessions(studyGoal.getTargetSessions())
                .status(studyGoal.getStatus())
                .currentHours(studyGoal.getCurrentHours())
                .currentMinutes(studyGoal.getCurrentMinutes())
                .currentSessions(studyGoal.getCurrentSessions())
                .progressRate(studyGoal.getProgressRate())
                .remainingHours(studyGoal.getRemainingHours())
                .remainingMinutes(studyGoal.getRemainingMinutes())
                .createdAt(studyGoal.getCreatedAt())
                .updatedAt(studyGoal.getUpdatedAt())
                .build();
    }
}