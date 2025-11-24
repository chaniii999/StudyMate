package studyMate.exception;

/**
 * 타이머를 찾을 수 없을 때 발생하는 예외
 */
public class TimerNotFoundException extends EntityNotFoundException {
    
    public TimerNotFoundException(Long timerId) {
        super("타이머", timerId);
    }
}

