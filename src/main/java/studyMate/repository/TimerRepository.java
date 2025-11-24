package studyMate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import studyMate.entity.StudyGoal;
import studyMate.entity.Timer;
import studyMate.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TimerRepository extends JpaRepository<Timer, Long> {

    List<Timer> findByUserOrderByStartTimeDesc(User user);
    
    List<Timer> findByUserAndStartTimeBetween(User user, LocalDateTime startTime, LocalDateTime endTime);
    
    // 학습목표별 타이머 기록 조회
    List<Timer> findByStudyGoalOrderByStartTimeDesc(StudyGoal studyGoal);
    
    // 학습목표의 특정 기간 타이머 기록 조회
    @Query("SELECT t FROM Timer t WHERE t.studyGoal = :studyGoal " +
           "AND DATE(t.createdAt) BETWEEN :startDate AND :endDate " +
           "ORDER BY t.createdAt DESC")
    List<Timer> findByStudyGoalAndDateRange(@Param("studyGoal") StudyGoal studyGoal,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);
    
    // 사용자의 학습목표별 타이머 기록 조회
    List<Timer> findByUserAndStudyGoalOrderByCreatedAtDesc(User user, StudyGoal studyGoal);
    
    // 사용자의 특정 기간 타이머 기록 조회 (학습목표별 필터링 포함)
    @Query("SELECT t FROM Timer t WHERE t.user = :user " +
           "AND (:studyGoal IS NULL OR t.studyGoal = :studyGoal) " +
           "AND DATE(t.createdAt) BETWEEN :startDate AND :endDate " +
           "ORDER BY t.createdAt DESC")
    List<Timer> findByUserAndOptionalStudyGoalAndDateRange(@Param("user") User user,
                                                          @Param("studyGoal") StudyGoal studyGoal,
                                                          @Param("startDate") LocalDate startDate,
                                                          @Param("endDate") LocalDate endDate);
    
    // 사용자의 전체 타이머 기록 조회
    List<Timer> findByUser(User user);
    
    // 사용자의 타이머 세션 수 카운트
    int countByUser(User user);
    
    // 사용자의 학습목표별 타이머 세션 수 카운트
    int countByUserAndStudyGoal(User user, StudyGoal studyGoal);
    
    // === 집계 쿼리 (성능 최적화) ===
    
    // 사용자의 총 학습시간 합계 (초 단위)
    @Query("SELECT COALESCE(SUM(t.studyTime), 0) FROM Timer t WHERE t.user = :user")
    int sumStudyTimeByUser(@Param("user") User user);
    
    // 사용자의 최장 학습 세션 시간 (초 단위)
    @Query("SELECT COALESCE(MAX(t.studyTime), 0) FROM Timer t WHERE t.user = :user")
    int maxStudyTimeByUser(@Param("user") User user);
    
    // 사용자의 평균 학습 세션 시간 (초 단위)
    @Query("SELECT COALESCE(AVG(t.studyTime), 0) FROM Timer t WHERE t.user = :user")
    double avgStudyTimeByUser(@Param("user") User user);
}