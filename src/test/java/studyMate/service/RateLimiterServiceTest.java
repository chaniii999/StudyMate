package studyMate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import studyMate.config.OpenAiProperties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimiterService 테스트")
class RateLimiterServiceTest {

    @Mock
    private OpenAiProperties openAiProperties;

    @Mock
    private OpenAiProperties.RateLimit rateLimit;

    @InjectMocks
    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        // setUp에서는 기본 설정만
    }

    @Test
    @DisplayName("Rate Limit 내에서 요청 허용")
    void canMakeRequest_WithinLimit_ReturnsTrue() {
        // Given
        when(openAiProperties.getRateLimit()).thenReturn(rateLimit);
        when(rateLimit.getRequestsPerMinute()).thenReturn(20);

        // When
        boolean result = rateLimiterService.canMakeRequest();

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("Rate Limit 초과 시 요청 거부")
    void canMakeRequest_ExceedsLimit_ReturnsFalse() {
        // Given
        when(openAiProperties.getRateLimit()).thenReturn(rateLimit);
        when(rateLimit.getRequestsPerMinute()).thenReturn(20);
        
        // Rate limit을 초과하도록 여러 요청
        int maxRequests = 20;
        for (int i = 0; i < maxRequests; i++) {
            rateLimiterService.canMakeRequest();
        }

        // When
        boolean result = rateLimiterService.canMakeRequest();

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("현재 요청 수 조회")
    void getCurrentRequestCount_ReturnsCount() {
        // Given
        when(openAiProperties.getRateLimit()).thenReturn(rateLimit);
        when(rateLimit.getRequestsPerMinute()).thenReturn(20);
        
        rateLimiterService.canMakeRequest();
        rateLimiterService.canMakeRequest();

        // When
        int count = rateLimiterService.getCurrentRequestCount();

        // Then
        assertEquals(2, count);
    }

    @Test
    @DisplayName("최대 요청 수 조회")
    void getMaxRequestsPerMinute_ReturnsMaxRequests() {
        // Given
        when(openAiProperties.getRateLimit()).thenReturn(rateLimit);
        when(rateLimit.getRequestsPerMinute()).thenReturn(20);

        // When
        int maxRequests = rateLimiterService.getMaxRequestsPerMinute();

        // Then
        assertEquals(20, maxRequests);
    }

    @Test
    @DisplayName("Rate Limit 초과 시 거부 확인")
    void canMakeRequest_ExceedsLimit_ConfirmsRejection() {
        // Given
        when(openAiProperties.getRateLimit()).thenReturn(rateLimit);
        when(rateLimit.getRequestsPerMinute()).thenReturn(20);
        
        // Rate limit까지 요청
        int maxRequests = 20;
        for (int i = 0; i < maxRequests; i++) {
            rateLimiterService.canMakeRequest();
        }

        // When - 추가 요청 시도
        boolean result = rateLimiterService.canMakeRequest();

        // Then - Rate limit 초과로 거부됨
        // 참고: 실제 시간 경과 테스트는 LocalDateTime.now()를 사용하므로 단위 테스트에서는 어려움
        assertFalse(result);
    }
}

