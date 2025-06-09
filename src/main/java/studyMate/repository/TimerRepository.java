package studyMate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import studyMate.entity.Timer;

@Repository
public interface TimerRepository extends JpaRepository <Timer, Long> {


}
