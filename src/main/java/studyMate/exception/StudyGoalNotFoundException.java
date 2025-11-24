package studyMate.exception;

/**
 * 학습목표를 찾을 수 없을 때 발생하는 예외
 */
public class StudyGoalNotFoundException extends EntityNotFoundException {
    
    public StudyGoalNotFoundException(Long goalId) {
        super("학습목표", goalId);
    }
}

