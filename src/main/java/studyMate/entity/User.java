package studyMate.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.f4b6a3.ulid.UlidCreator;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(length = 26, updatable = false, nullable = false)
    private String id;

    @Column(length = 40, nullable = false, unique = true)
    private String email;

    @Column(length = 255, nullable = false)
    private String password;

    @Column(length = 15, nullable = false)
    private String nickname;

    private Integer age;

    @Column(nullable = false)
    private String sex;

    // 요약 캐시 : 갱신/재계산 로직 필요
    @Column(name = "total_study_time", nullable = false)
    private int totalStudyTime = 0;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @ToString.Exclude
    private List<StudyTopic> studyTopics = new ArrayList<>();

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    // 최근 접속일자
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

    // 연관관계 편의 메서드
    public void addStudyTopic(StudyTopic studyTopic) {
        this.studyTopics.add(studyTopic);
        studyTopic.setUser(this);
    }

    public void removeStudyTopic(StudyTopic studyTopic) {
        this.studyTopics.remove(studyTopic);
        studyTopic.setUser(null);
    }
}
