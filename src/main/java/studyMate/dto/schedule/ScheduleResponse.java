package studyMate.dto.schedule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import studyMate.entity.Schedule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleResponse {
    // === 기본 정보 ===
    private String id;
    private String title;
    private String subtitle;
    private String description;
    private String color;

    // === 날짜/시간 정보 ===
    private LocalDate scheduleDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean isAllDay;
    private boolean isRecurring;
    private String recurrenceRule;

    // === 학습 관련 정보 ===
    private String studyMode;
    private Integer plannedStudyMinutes;
    private Integer plannedBreakMinutes;
    private String studyGoal;
    private String difficulty;

    // === 상태 정보 ===
    private Schedule.ScheduleStatus status;
    private Integer completionRate;
    private boolean isOverdue;

    // === 알림 설정 ===
    private Integer reminderMinutes;
    private boolean isReminderEnabled;

    // === 연관 정보 ===
    private StudyTopicResponse topic;

    // === 학습 통계 ===
    private Integer totalStudyTime; // 총 학습 시간 (초)
    private Integer totalRestTime; // 총 휴식 시간 (초)
    private Integer actualStudyMinutes; // 실제 학습 시간 (분)
    private Integer actualRestMinutes; // 실제 휴식 시간 (분)

    // === AI 정보 ===
    private String aiSummary;
    private String aiSuggestions;

    // === 메타데이터 ===
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // === 내부 클래스 ===
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudyTopicResponse {
        private String id;
        private String name;
    }

    // === 정적 팩토리 메서드 ===
    public static ScheduleResponse from(Schedule schedule) {
        return ScheduleResponse.builder()
                .id(schedule.getId())
                .title(schedule.getTitle())
                .subtitle(schedule.getSubtitle())
                .description(schedule.getDescription())
                .color(schedule.getColor())
                .scheduleDate(schedule.getScheduleDate())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .isAllDay(schedule.isAllDay())
                .isRecurring(schedule.isRecurring())
                .recurrenceRule(schedule.getRecurrenceRule())
                .studyMode(schedule.getStudyMode())
                .plannedStudyMinutes(schedule.getPlannedStudyMinutes())
                .plannedBreakMinutes(schedule.getPlannedBreakMinutes())
                .studyGoal(schedule.getStudyGoal())
                .difficulty(schedule.getDifficulty())
                .status(schedule.getStatus())
                .completionRate(schedule.getCompletionRate())
                .isOverdue(schedule.isOverdue())
                .reminderMinutes(schedule.getReminderMinutes())
                .isReminderEnabled(schedule.isReminderEnabled())
                .topic(schedule.getTopic() != null ? 
                    StudyTopicResponse.builder()
                        .id(schedule.getTopic().getId())
                        .name(schedule.getTopic().getName())
                        .build() : null)
                .totalStudyTime(schedule.getTotalStudyTime())
                .totalRestTime(schedule.getTotalRestTime())
                .actualStudyMinutes(schedule.getTotalStudyTime() > 0 ? schedule.getTotalStudyTime() / 60 : 0)
                .actualRestMinutes(schedule.getTotalRestTime() > 0 ? schedule.getTotalRestTime() / 60 : 0)
                .aiSummary(schedule.getAiSummary())
                .aiSuggestions(schedule.getAiSuggestions())
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt())
                .build();
    }
} 