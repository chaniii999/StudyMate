package studyMate.exception;

/**
 * 이메일이 이미 존재할 때 발생하는 예외
 */
public class EmailAlreadyExistsException extends ValidationException {
    
    public EmailAlreadyExistsException(String email) {
        super(String.format("이미 사용 중인 이메일입니다: %s", email));
    }
}

