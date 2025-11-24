package studyMate.exception;

/**
 * AI 서비스 관련 예외
 */
public class AiServiceException extends StudyMateException {
    
    public AiServiceException(String message) {
        super(message);
    }
    
    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

