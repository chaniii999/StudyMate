package studyMate.dto.studygoal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import studyMate.entity.GoalStatus;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyGoalRequest {
    
    private String title;           // 목표명
    private String subject;         // 과목
    private String description;     // 상세 설명
    private String color;           // 색상 테마
    private LocalDate startDate;    // 시작일
    private LocalDate targetDate;   // 목표일
    private Integer targetHours;    // 목표 시간
    private Integer targetSessions; // 목표 세션 수
    private GoalStatus status;      // 상태
}