package studyMate.exception;

/**
 * 학습 시간이 너무 짧을 때 발생하는 예외
 */
public class StudyTimeTooShortException extends ValidationException {
    
    public StudyTimeTooShortException(int studyTimeSeconds, int minimumSeconds) {
        super(String.format("학습 시간이 너무 짧습니다. 현재: %d초(%d분), 최소 필요: %d초(%d분)", 
                studyTimeSeconds, studyTimeSeconds / 60, minimumSeconds, minimumSeconds / 60));
    }
}

