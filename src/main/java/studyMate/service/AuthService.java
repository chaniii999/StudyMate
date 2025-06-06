package studyMate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import studyMate.repository.UserRepository;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;

    private static final long CODE_TTL_MINUTES = 3; // 인증코드 유효 시간 (3분)

    private boolean isValidEmail(String email) {
        return isValidEmailFormat(email) && !userRepository.existsByEmail(email);
    }

    // 이메일 인증 코드 발송
    public void sendCode(String email) {
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("유효하지 않은 이메일 형식이거나 중복된 이메일입니다.");
        }

        String code = UUID.randomUUID().toString().substring(0, 6); // 랜덤 6자리 코드

        // Redis에 인증 코드 저장 (TTL 설정)
        redisTemplate.opsForValue().set("email:code:" + email, code, CODE_TTL_MINUTES, TimeUnit.MINUTES);

        // 이메일 전송
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[StudyMate] 이메일 인증 코드");
        message.setText("인증 코드: " + code);
        mailSender.send(message);
    }

    // 이메일 인증 코드 검증
    public boolean verifyCode(String email, String inputCode) {
        String storedCode = redisTemplate.opsForValue().get("email:code:" + email);
        return storedCode != null && storedCode.equals(inputCode);
    }

    // 이메일 정규식 검사
    private boolean isValidEmailFormat(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }
}
