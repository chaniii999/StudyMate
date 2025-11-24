package studyMate.exception;

/**
 * 스케줄을 찾을 수 없을 때 발생하는 예외
 */
public class ScheduleNotFoundException extends EntityNotFoundException {
    
    public ScheduleNotFoundException(String scheduleId) {
        super("스케줄", scheduleId);
    }
}

