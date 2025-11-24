package studyMate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import studyMate.dto.TokenDto;
import studyMate.exception.InvalidTokenException;
import studyMate.repository.UserRepository;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisService;

    private static final long CODE_TTL_MINUTES = 3; // 인증코드 유효 시간 (3분)
    private static final long VERIFIED_TTL_MINUTES = 30; // 인증완료 상태 유효 시간 (30분)
    private static final SecureRandom secureRandom = new SecureRandom(); // 보안 강화된 랜덤 생성기

    private boolean isValidEmail(String email) {
        boolean formatOk = isValidEmailFormat(email);
        boolean notExists = !userRepository.existsByEmail(email);
        return formatOk && notExists;
    }

    // 이메일 인증 코드 발송 (보안 강화: SecureRandom 사용)
    public void sendCode(String email) {
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("유효하지 않은 이메일 형식이거나 중복된 이메일입니다.");
        }

        // SecureRandom을 사용한 6자리 숫자 코드 생성 (보안 강화)
        int code = 100000 + secureRandom.nextInt(900000); // 100000 ~ 999999
        String codeString = String.valueOf(code);
        
        // Redis에 인증 코드 저장 (TTL 설정)
        redisTemplate.opsForValue().set("email:code:" + email, codeString, CODE_TTL_MINUTES, TimeUnit.MINUTES);
        log.info("이메일 인증 코드 생성: {} -> {}", email, codeString);
        
        // 이메일 전송
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[StudyMate] 이메일 인증 코드");
        message.setText("인증 코드: " + codeString + "\n이 코드는 " + CODE_TTL_MINUTES + "분 동안 유효합니다.");
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

    public TokenDto refreshToken(String refreshToken) {
        // 리프레시 토큰 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        // 리프레시 토큰에서 사용자 이메일 추출
        String email = jwtTokenProvider.getUsername(refreshToken);
        
        // Redis에서 저장된 리프레시 토큰 확인
        String savedRefreshToken = redisService.getRefreshToken(email);
        if (savedRefreshToken == null || !savedRefreshToken.equals(refreshToken)) {
            throw new InvalidTokenException("Refresh token not found or not matched");
        }

        // 새로운 액세스 토큰 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(email);
        
        // 새로운 리프레시 토큰 발급 (선택사항 - 리프레시 토큰 재사용 정책에 따라)
        String newRefreshToken = jwtTokenProvider.createRefreshToken(email);
        
        // Redis에 새로운 리프레시 토큰 저장
        redisService.saveRefreshToken(email, newRefreshToken, 
            jwtTokenProvider.getRefreshTokenExpirationTime());

        return TokenDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }
}
