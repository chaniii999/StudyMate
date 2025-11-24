package studyMate.exception;

/**
 * 접근 권한이 없을 때 발생하는 예외
 */
public class AccessDeniedException extends StudyMateException {
    
    public AccessDeniedException(String resource, Object resourceId, Object userId) {
        super(String.format("%s(%s)에 접근할 권한이 없습니다. (사용자: %s)", resource, resourceId, userId));
    }
    
    public AccessDeniedException(String message) {
        super(message);
    }
}

