package studyMate.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.f4b6a3.ulid.UlidCreator;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "study_topics")
public class StudyTopic {

    @Id
    @Column(length = 26, nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore // Jackson 순환 참조 방지 (선택)
    @ToString.Exclude // lombok 순환 방지 (선택)
    private User user;

    @Column(length = 50, nullable = false)
    private String name; // 공부 제목 (예: 리액트, 영어단어)

    @Column(length = 255)
    private String goal; // 공부 목표

    @Column(name = "total_study_time", nullable = false)
    private int totalStudyTime = 0; // 단위: 분

    @Column(name = "total_study_count", nullable = false)
    private int totalStudyCount = 0; // 공부한 횟수

    @Lob
    @Column(columnDefinition = "TEXT")
    private String strategy; // 공부 전략 or 프롬프트

    @Lob
    @Column(columnDefinition = "TEXT")
    private String summary; // AI 요약

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

    // 기본 생성자, getter/setter 필요시 추가
}
