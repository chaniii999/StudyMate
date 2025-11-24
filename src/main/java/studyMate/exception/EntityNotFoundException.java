package studyMate.exception;

/**
 * 엔티티를 찾을 수 없을 때 발생하는 예외
 */
public class EntityNotFoundException extends StudyMateException {
    
    public EntityNotFoundException(String entityName, Object id) {
        super(String.format("%s을(를) 찾을 수 없습니다: %s", entityName, id));
    }
    
    public EntityNotFoundException(String message) {
        super(message);
    }
}

