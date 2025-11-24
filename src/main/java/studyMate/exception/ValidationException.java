package studyMate.exception;

/**
 * 데이터 검증 실패 시 발생하는 예외
 */
public class ValidationException extends StudyMateException {
    
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String field, String reason) {
        super(String.format("%s 검증 실패: %s", field, reason));
    }
}

