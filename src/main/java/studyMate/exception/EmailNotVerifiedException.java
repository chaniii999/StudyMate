package studyMate.exception;

/**
 * 이메일 인증이 완료되지 않았을 때 발생하는 예외
 */
public class EmailNotVerifiedException extends ValidationException {
    
    public EmailNotVerifiedException(String email) {
        super(String.format("이메일 인증이 완료되지 않았습니다: %s", email));
    }
}

