package studyMate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import studyMate.entity.Timer;
import studyMate.entity.User;
import java.util.List;

@Repository
public interface TimerRepository extends JpaRepository <Timer, Long> {

    List<Timer> findByUserOrderByStartTimeDesc(User user);

}
