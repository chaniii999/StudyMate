package studyMate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.javamail.JavaMailSender;
import studyMate.dto.TokenDto;
import studyMate.exception.InvalidTokenException;
import studyMate.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 테스트")
class AuthServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RedisService redisService;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    private String email;
    private String validRefreshToken;

    @BeforeEach
    void setUp() {
        email = "test@example.com";
        validRefreshToken = "valid.refresh.token";
    }

    @Test
    @DisplayName("유효한 이메일로 인증 코드 발송 성공")
    void sendCode_ValidEmail_Success() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(userRepository.existsByEmail(email)).thenReturn(false);

        // When
        authService.sendCode(email);

        // Then
        verify(userRepository, times(1)).existsByEmail(email);
        verify(valueOperations, times(1)).set(eq("email:code:" + email), anyString(), 
                eq(3L), any());
        verify(mailSender, times(1)).send(any(org.springframework.mail.SimpleMailMessage.class));
    }

    @Test
    @DisplayName("중복된 이메일로 인증 코드 발송 시 예외 발생")
    void sendCode_DuplicateEmail_ThrowsException() {
        // Given
        when(userRepository.existsByEmail(email)).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.sendCode(email);
        });

        assertTrue(exception.getMessage().contains("중복된 이메일"));
        verify(mailSender, never()).send(any(org.springframework.mail.SimpleMailMessage.class));
    }

    @Test
    @DisplayName("유효하지 않은 이메일 형식으로 인증 코드 발송 시 예외 발생")
    void sendCode_InvalidEmailFormat_ThrowsException() {
        // Given
        String invalidEmail = "invalid-email";

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.sendCode(invalidEmail);
        });

        assertTrue(exception.getMessage().contains("유효하지 않은 이메일"));
        verify(mailSender, never()).send(any(org.springframework.mail.SimpleMailMessage.class));
    }

    @Test
    @DisplayName("올바른 인증 코드로 검증 성공")
    void verifyCode_ValidCode_Success() {
        // Given
        String code = "123456";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("email:code:" + email)).thenReturn(code);

        // When
        authService.verifyCode(email, code);

        // Then
        verify(valueOperations, times(1)).get("email:code:" + email);
        verify(valueOperations, times(1)).set(eq("email:verified:" + email), eq("true"), 
                eq(30L), any());
        verify(redisTemplate, times(1)).delete("email:code:" + email);
    }

    @Test
    @DisplayName("잘못된 인증 코드로 검증 시 예외 발생")
    void verifyCode_InvalidCode_ThrowsException() {
        // Given
        String storedCode = "123456";
        String inputCode = "000000";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("email:code:" + email)).thenReturn(storedCode);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.verifyCode(email, inputCode);
        });

        assertTrue(exception.getMessage().contains("인증 코드가 일치하지 않거나 만료"));
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("만료된 인증 코드로 검증 시 예외 발생")
    void verifyCode_ExpiredCode_ThrowsException() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("email:code:" + email)).thenReturn(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.verifyCode(email, "123456");
        });

        assertTrue(exception.getMessage().contains("인증 코드가 일치하지 않거나 만료"));
    }

    @Test
    @DisplayName("이메일 인증 여부 확인 - 인증 완료")
    void isEmailVerified_Verified_ReturnsTrue() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("email:verified:" + email)).thenReturn("true");

        // When
        boolean result = authService.isEmailVerified(email);

        // Then
        assertTrue(result);
        verify(valueOperations, times(1)).get("email:verified:" + email);
    }

    @Test
    @DisplayName("이메일 인증 여부 확인 - 미인증")
    void isEmailVerified_NotVerified_ReturnsFalse() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("email:verified:" + email)).thenReturn(null);

        // When
        boolean result = authService.isEmailVerified(email);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("유효한 리프레시 토큰으로 토큰 갱신 성공")
    void refreshToken_ValidToken_Success() {
        // Given
        String newAccessToken = "new.access.token";
        String newRefreshToken = "new.refresh.token";
        when(jwtTokenProvider.validateToken(validRefreshToken)).thenReturn(true);
        when(jwtTokenProvider.getUsername(validRefreshToken)).thenReturn(email);
        when(redisService.getRefreshToken(email)).thenReturn(validRefreshToken);
        when(jwtTokenProvider.createAccessToken(email)).thenReturn(newAccessToken);
        when(jwtTokenProvider.createRefreshToken(email)).thenReturn(newRefreshToken);
        when(jwtTokenProvider.getRefreshTokenExpirationTime()).thenReturn(86400L);

        // When
        TokenDto result = authService.refreshToken(validRefreshToken);

        // Then
        assertNotNull(result);
        assertEquals(newAccessToken, result.getAccessToken());
        assertEquals(newRefreshToken, result.getRefreshToken());
        verify(jwtTokenProvider, times(1)).validateToken(validRefreshToken);
        verify(redisService, times(1)).saveRefreshToken(eq(email), eq(newRefreshToken), eq(86400L));
    }

    @Test
    @DisplayName("유효하지 않은 리프레시 토큰으로 갱신 시 예외 발생")
    void refreshToken_InvalidToken_ThrowsException() {
        // Given
        String invalidToken = "invalid.token";
        when(jwtTokenProvider.validateToken(invalidToken)).thenReturn(false);

        // When & Then
        InvalidTokenException exception = assertThrows(InvalidTokenException.class, () -> {
            authService.refreshToken(invalidToken);
        });

        assertEquals("Invalid refresh token", exception.getMessage());
        verify(redisService, never()).getRefreshToken(anyString());
    }

    @Test
    @DisplayName("Redis에 저장되지 않은 리프레시 토큰으로 갱신 시 예외 발생")
    void refreshToken_TokenNotInRedis_ThrowsException() {
        // Given
        when(jwtTokenProvider.validateToken(validRefreshToken)).thenReturn(true);
        when(jwtTokenProvider.getUsername(validRefreshToken)).thenReturn(email);
        when(redisService.getRefreshToken(email)).thenReturn(null);

        // When & Then
        InvalidTokenException exception = assertThrows(InvalidTokenException.class, () -> {
            authService.refreshToken(validRefreshToken);
        });

        assertTrue(exception.getMessage().contains("Refresh token not found"));
        verify(jwtTokenProvider, never()).createAccessToken(anyString());
    }
}

