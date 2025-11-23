package studyMate.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.f4b6a3.ulid.UlidCreator;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "schedules", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_schedule_date", columnList = "schedule_date"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
public class Schedule {

    @Id
    @Column(length = 26, updatable = false, nullable = false)
    private String id;

    @JsonIgnore // 순환 참조 방지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user; // 해당 유저

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    @ToString.Exclude
    private StudyTopic topic; // 학습 주제

    // === 기본 일정 정보 ===
    @Column(length = 255, nullable = false)
    private String title; // 일정 제목

    @Column(length = 500)
    private String subtitle; // 일정 소제목

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description; // 일정 설명

    @Column(length = 7)
    private String color; // 캘린더 표시 색상 (예: "#FF5733")

    // === 일정 날짜/시간 정보 ===
    @Column(name = "schedule_date", nullable = false)
    private LocalDate scheduleDate; // 일정 날짜

    @Column(name = "start_time")
    private LocalTime startTime; // 시작 시간 (null이면 종일 일정)

    @Column(name = "end_time")
    private LocalTime endTime; // 종료 시간 (null이면 종일 일정)

    @Column(name = "is_all_day", nullable = false)
    @Builder.Default
    private boolean isAllDay = false; // 종일 일정 여부

    @Column(name = "is_recurring", nullable = false)
    @Builder.Default
    private boolean isRecurring = false; // 반복 일정 여부

    @Column(length = 20)
    private String recurrenceRule; // 반복 규칙 (예: "DAILY", "WEEKLY", "MONTHLY")

    // === 학습 관련 정보 ===
    @Column(length = 20)
    private String studyMode; // 학습 모드 (예: "POMODORO", "FOCUS", "REVIEW")

    @Column(name = "planned_study_minutes")
    private Integer plannedStudyMinutes; // 계획된 학습 시간 (분)

    @Column(name = "planned_break_minutes")
    private Integer plannedBreakMinutes; // 계획된 휴식 시간 (분)
  
    @Column(length = 50)
    private String studyGoal; // 학습 목표

    @Column(length = 20)
    private String difficulty; // 예상 난이도 (EASY, MEDIUM, HARD)

    // === 상태 정보 ===
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private ScheduleStatus status = ScheduleStatus.PLANNED; // 일정 상태

    @Column(name = "completion_rate")
    @Builder.Default
    private Integer completionRate = 0; // 완료율 (0-100)

    // === 알림 설정 ===
    @Column(name = "reminder_minutes")
    private Integer reminderMinutes; // 알림 시간 (분 전)

    @Column(name = "is_reminder_enabled", nullable = false)
    @Builder.Default
    private boolean isReminderEnabled = true; // 알림 활성화 여부

    // === Timer 연관관계 ===
    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @ToString.Exclude
    @Builder.Default
    private List<Timer> timers = new ArrayList<>(); // 실제 학습 기록들

    // === AI 관련 정보 ===
    @Lob
    @Column(columnDefinition = "TEXT")
    private String aiSummary; // AI 요약

    @Lob
    @Column(columnDefinition = "TEXT")
    private String aiSuggestions; // AI 개선 제안

    // === 메타데이터 ===
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UlidCreator.getUlid().toString();
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // === 연관관계 편의 메서드 ===
    public void addTimer(Timer timer) {
        this.timers.add(timer);
        timer.setSchedule(this);
    }

    public void removeTimer(Timer timer) {
        this.timers.remove(timer);
        timer.setSchedule(null);
    }

    // === 비즈니스 로직 메서드 ===
    public boolean isCompleted() {
        return ScheduleStatus.COMPLETED.equals(this.status);
    }

    public boolean isInProgress() {
        return ScheduleStatus.IN_PROGRESS.equals(this.status);
    }

    public boolean isOverdue() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduleDateTime = this.scheduleDate.atTime(
            this.startTime != null ? this.startTime : LocalTime.MIN
        );
        return now.isAfter(scheduleDateTime) && !this.isCompleted();
    }

    public int getTotalStudyTime() {
        return this.timers.stream()
                .mapToInt(Timer::getStudyTime)
                .sum();
    }

    public int getTotalRestTime() {
        return this.timers.stream()
                .mapToInt(Timer::getRestTime)
                .sum();
    }

    // === 업데이트 메서드 ===
    public void updateFromRequest(studyMate.dto.schedule.ScheduleRequest request, StudyTopic topic) {
        if (request.getTitle() != null) this.title = request.getTitle();
        if (request.getSubtitle() != null) this.subtitle = request.getSubtitle();
        if (request.getDescription() != null) this.description = request.getDescription();
        if (request.getColor() != null) this.color = request.getColor();
        if (request.getScheduleDate() != null) this.scheduleDate = request.getScheduleDate();
        if (request.getStartTime() != null) this.startTime = request.getStartTime();
        if (request.getEndTime() != null) this.endTime = request.getEndTime();
        if (request.getIsAllDay() != null) this.isAllDay = request.getIsAllDay();
        if (request.getIsRecurring() != null) this.isRecurring = request.getIsRecurring();
        if (request.getRecurrenceRule() != null) this.recurrenceRule = request.getRecurrenceRule();
        if (request.getStudyMode() != null) this.studyMode = request.getStudyMode();
        if (request.getPlannedStudyMinutes() != null) this.plannedStudyMinutes = request.getPlannedStudyMinutes();
        if (request.getPlannedBreakMinutes() != null) this.plannedBreakMinutes = request.getPlannedBreakMinutes();
        if (request.getStudyGoal() != null) this.studyGoal = request.getStudyGoal();
        if (request.getDifficulty() != null) this.difficulty = request.getDifficulty();
        if (request.getReminderMinutes() != null) this.reminderMinutes = request.getReminderMinutes();
        if (request.getIsReminderEnabled() != null) this.isReminderEnabled = request.getIsReminderEnabled();
        this.topic = topic; // topic은 null일 수 있으므로 항상 업데이트
    }

    // === 일정 상태 열거형 ===
    public enum ScheduleStatus {
        PLANNED,      // 계획됨
        IN_PROGRESS,  // 진행 중
        COMPLETED,    // 완료됨
        CANCELLED,    // 취소됨
        POSTPONED     // 연기됨
    }
}
