package studyMate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import studyMate.entity.Schedule;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {



}
