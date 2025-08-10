package studyMate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import studyMate.entity.GoalStatus;
import studyMate.entity.StudyGoal;
import studyMate.entity.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudyGoalRepository extends JpaRepository<StudyGoal, Long> {
    
    // 사용자의 모든 학습목표 조회
    List<StudyGoal> findByUserOrderByCreatedAtDesc(User user);
    
    // 사용자의 활성 학습목표 조회
    List<StudyGoal> findByUserAndStatusOrderByCreatedAtDesc(User user, GoalStatus status);
    
    // 사용자의 특정 학습목표 조회
    Optional<StudyGoal> findByIdAndUser(Long id, User user);
    
    // 사용자의 과목별 학습목표 조회
    List<StudyGoal> findByUserAndSubjectOrderByCreatedAtDesc(User user, String subject);
    
    // 사용자의 기간별 학습목표 조회
    @Query("SELECT sg FROM StudyGoal sg WHERE sg.user = :user " +
           "AND sg.startDate <= :endDate AND sg.targetDate >= :startDate " +
           "ORDER BY sg.createdAt DESC")
    List<StudyGoal> findByUserAndDateRange(@Param("user") User user, 
                                          @Param("startDate") LocalDate startDate, 
                                          @Param("endDate") LocalDate endDate);
    
    // 사용자의 완료된 학습목표 수 조회
    @Query("SELECT COUNT(sg) FROM StudyGoal sg WHERE sg.user = :user AND sg.status = 'COMPLETED'")
    Long countCompletedGoalsByUser(@Param("user") User user);
    
    // 사용자의 총 학습 시간 조회
    @Query("SELECT COALESCE(SUM(sg.currentHours), 0) FROM StudyGoal sg WHERE sg.user = :user")
    Integer getTotalStudyHoursByUser(@Param("user") User user);
}