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

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisService 테스트")
class RedisServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisService redisService;

    private String email;
    private String refreshToken;

    @BeforeEach
    void setUp() {
        email = "test@example.com";
        refreshToken = "refresh.token.here";
    }

    @Test
    @DisplayName("리프레시 토큰 저장 성공")
    void saveRefreshToken_Success() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        long expirationTime = 86400L; // 24시간

        // When
        redisService.saveRefreshToken(email, refreshToken, expirationTime);

        // Then
        verify(valueOperations, times(1)).set(
                eq("RT:" + email),
                eq(refreshToken),
                eq(expirationTime),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    @DisplayName("리프레시 토큰 조회 성공")
    void getRefreshToken_Success() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("RT:" + email)).thenReturn(refreshToken);

        // When
        String result = redisService.getRefreshToken(email);

        // Then
        assertNotNull(result);
        assertEquals(refreshToken, result);
        verify(valueOperations, times(1)).get("RT:" + email);
    }

    @Test
    @DisplayName("저장되지 않은 리프레시 토큰 조회 시 null 반환")
    void getRefreshToken_NotExists_ReturnsNull() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("RT:" + email)).thenReturn(null);

        // When
        String result = redisService.getRefreshToken(email);

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("리프레시 토큰 삭제 성공")
    void deleteRefreshToken_Success() {
        // When
        redisService.deleteRefreshToken(email);

        // Then
        verify(redisTemplate, times(1)).delete("RT:" + email);
    }

    @Test
    @DisplayName("다른 이메일의 토큰 저장 및 조회")
    void saveAndGetRefreshToken_DifferentEmails_Success() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String email1 = "user1@example.com";
        String email2 = "user2@example.com";
        String token1 = "token1";
        String token2 = "token2";
        long expirationTime = 86400L;

        when(valueOperations.get("RT:" + email1)).thenReturn(token1);
        when(valueOperations.get("RT:" + email2)).thenReturn(token2);

        // When
        redisService.saveRefreshToken(email1, token1, expirationTime);
        redisService.saveRefreshToken(email2, token2, expirationTime);
        String result1 = redisService.getRefreshToken(email1);
        String result2 = redisService.getRefreshToken(email2);

        // Then
        assertEquals(token1, result1);
        assertEquals(token2, result2);
        verify(valueOperations, times(2)).set(anyString(), anyString(), eq(expirationTime), eq(TimeUnit.SECONDS));
    }
}

