package studyMate.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "timers", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
public class Timer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    @ToString.Exclude
    private Schedule schedule; // 연관된 스케줄 (선택적)

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "study_seconds", nullable = false)
    private int studyTime; // 실제 학습 시간 (초)

    @Column(name = "rest_seconds", nullable = false)
    private int restTime; // 실제 휴식 시간 (초)

    @Column(length = 20)
    private String mode; // 예: "25/5", "50/10"

    @Lob
    @Column(columnDefinition = "TEXT")
    private String summary; // AI 요약

    @Lob
    @Column(columnDefinition = "TEXT")
    private String aiFeedback; // AI 피드백

    @Lob
    @Column(columnDefinition = "TEXT")
    private String aiSuggestions; // AI 개선 제안

    @Lob
    @Column(columnDefinition = "TEXT")
    private String aiMotivation; // AI 동기부여 메시지

    @Column(name = "ai_feedback_created_at")
    private LocalDateTime aiFeedbackCreatedAt; // AI 피드백 생성 시간

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

}
