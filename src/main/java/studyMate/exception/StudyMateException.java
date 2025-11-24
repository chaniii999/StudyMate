package studyMate.exception;

/**
 * StudyMate 애플리케이션의 기본 예외 클래스
 * 모든 커스텀 예외의 상위 클래스
 */
public class StudyMateException extends RuntimeException {
    
    public StudyMateException(String message) {
        super(message);
    }
    
    public StudyMateException(String message, Throwable cause) {
        super(message, cause);
    }
}

