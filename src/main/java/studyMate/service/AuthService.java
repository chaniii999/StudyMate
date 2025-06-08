package studyMate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import studyMate.repository.UserRepository;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;

    private static final long CODE_TTL_MINUTES = 3; // 인증코드 유효 시간 (3분)
    private static final long VERIFIED_TTL_MINUTES = 30; // 인증완료 상태 유효 시간 (30분)

    private boolean isValidEmail(String email) {
        boolean formatOk = isValidEmailFormat(email);
        boolean notExists = !userRepository.existsByEmail(email);
        return formatOk && notExists;
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
        message.setText("인증 코드: " + code + "\n이 코드는 " + CODE_TTL_MINUTES + "분 동안 유효합니다.");
        mailSender.send(message);
    }

    public void verifyCode(String email, String inputCode) {
        String storedCode = redisTemplate.opsForValue().get("email:code:" + email);
        if (storedCode == null || !storedCode.equals(inputCode)) {
            throw new IllegalArgumentException("인증 코드가 일치하지 않거나 만료되었습니다.");
        }
        
        // 인증 성공 시 인증 완료 상태 저장
        redisTemplate.opsForValue().set("email:verified:" + email, "true", VERIFIED_TTL_MINUTES, TimeUnit.MINUTES);
        // 사용한 인증 코드 삭제
        redisTemplate.delete("email:code:" + email);
    }

    // 이메일 인증 여부 확인
    public boolean isEmailVerified(String email) {
        String verified = redisTemplate.opsForValue().get("email:verified:" + email);
        return "true".equals(verified);
    }

    private boolean isValidEmailFormat(String email) {
        return email != null && email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");
    }
}
