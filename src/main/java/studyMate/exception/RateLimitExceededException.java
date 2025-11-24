package studyMate.exception;

/**
 * Rate Limit 초과 시 발생하는 예외
 */
public class RateLimitExceededException extends StudyMateException {
    
    public RateLimitExceededException(String message) {
        super(message);
    }
    
    public RateLimitExceededException(int currentRequests, int maxRequests) {
        super(String.format("AI 서비스 사용량이 초과되었습니다. 현재: %d/%d 요청. 잠시 후 다시 시도해주세요.", 
                currentRequests, maxRequests));
    }
}

