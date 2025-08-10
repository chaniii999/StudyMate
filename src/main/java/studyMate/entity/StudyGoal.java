package studyMate.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "study_goals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudyGoal {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 기본 정보
    @Column(length = 100, nullable = false)
    private String title;           // 목표명 (예: "토익 900점 달성")
    
    @Column(length = 50, nullable = false)
    private String subject;         // 과목 (예: "영어", "수학", "프로그래밍")
    
    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;     // 상세 설명
    
    @Column(length = 7)
    private String color;           // 색상 테마 (hex)
    
    // 목표 설정
    @Column(nullable = false)
    private LocalDate startDate;    // 시작일
    
    @Column(nullable = false)
    private LocalDate targetDate;   // 목표일
    
    @Column(nullable = false)
    private Integer targetHours;    // 목표 시간 (시간)
    
    @Column
    private Integer targetSessions; // 목표 세션 수
    
    // 진행 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GoalStatus status;      // ACTIVE, COMPLETED, PAUSED, CANCELLED
    
    @Builder.Default
    @Column(nullable = false)
    private Integer currentHours = 0;   // 현재 진행 시간 (시간)
    
    @Builder.Default
    @Column(nullable = false)
    private Integer currentMinutes = 0; // 현재 진행 시간 (분, 정확한 진행도 계산용)
    
    @Builder.Default
    @Column(nullable = false)
    private Integer currentSessions = 0; // 현재 진행 세션
    
    // 사용자 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    // 연관관계
    @OneToMany(mappedBy = "studyGoal", cascade = CascadeType.ALL)
    private List<Timer> timerRecords;
    
    // 메타데이터
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // 진행률 계산 메서드 (분 단위 고려)
    public double getProgressRate() {
        if (targetHours == null || targetHours == 0) {
            return 0.0;
        }
        int targetTotalMinutes = targetHours * 60;
        return Math.min(100.0, (currentMinutes.doubleValue() / targetTotalMinutes) * 100.0);
    }
    
    // 남은 시간 계산 (분 단위)
    public int getRemainingMinutes() {
        int targetTotalMinutes = targetHours * 60;
        return Math.max(0, targetTotalMinutes - currentMinutes);
    }
    
    // 남은 시간 계산 (시간 단위, 하위 호환성)
    public int getRemainingHours() {
        return getRemainingMinutes() / 60;
    }
}