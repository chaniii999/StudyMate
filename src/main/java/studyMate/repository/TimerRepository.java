package studyMate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import studyMate.entity.Timer;
import studyMate.entity.User;
import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface TimerRepository extends JpaRepository <Timer, Long> {

    List<Timer> findByUserOrderByStartTimeDesc(User user);
    
    List<Timer> findByUserAndStartTimeBetween(User user, LocalDateTime startTime, LocalDateTime endTime);

}
