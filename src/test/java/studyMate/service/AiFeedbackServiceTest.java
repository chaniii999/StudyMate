package studyMate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import studyMate.dto.ai.AiFeedbackRequest;
import studyMate.dto.ai.AiFeedbackResponse;
import studyMate.entity.Timer;
import studyMate.entity.User;
import studyMate.exception.RateLimitExceededException;
import studyMate.exception.StudyTimeTooShortException;
import studyMate.exception.TimerNotFoundException;
import studyMate.repository.TimerRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiFeedbackService 테스트")
class AiFeedbackServiceTest {

    @Mock
    private WebClient openAiWebClient;

    @Mock
    private TimerRepository timerRepository;

    @Mock
    private RateLimiterService rateLimiterService;

    @InjectMocks
    private AiFeedbackService aiFeedbackService;

    private User user;
    private Timer timer;
    private AiFeedbackRequest request;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id("user1")
                .email("test@example.com")
                .nickname("테스터")
                .totalStudyTime(120) // 120분
                .build();

        timer = Timer.builder()
                .id(1L)
                .user(user)
                .studyTime(1800) // 30분 (초 단위)
                .restTime(300) // 5분
                .mode("STUDY")
                .summary("학습 요약")
                .startTime(LocalDateTime.now().minusMinutes(30))
                .endTime(LocalDateTime.now())
                .build();

        request = AiFeedbackRequest.builder()
                .timerId(1L)
                .studyTime(1800)
                .restTime(300)
                .mode("STUDY")
                .studySummary("학습 요약")
                .build();
    }

    @Test
    @DisplayName("Timer를 찾을 수 없으면 예외 발생")
    void getFeedback_TimerNotFound_ThrowsException() {
        // Given
        when(timerRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        TimerNotFoundException exception = assertThrows(TimerNotFoundException.class, () -> {
            aiFeedbackService.getFeedback(request);
        });

        assertTrue(exception.getMessage().contains("타이머") && exception.getMessage().contains("1"));
        verify(timerRepository, times(1)).findById(1L);
        verify(rateLimiterService, never()).canMakeRequest();
    }

    @Test
    @DisplayName("학습 시간이 2분(120초) 미만이면 예외 발생")
    void getFeedback_StudyTimeLessThan2Minutes_ThrowsException() {
        // Given
        timer.setStudyTime(100); // 100초 (2분 미만)
        when(timerRepository.findById(1L)).thenReturn(Optional.of(timer));

        // When & Then
        StudyTimeTooShortException exception = assertThrows(StudyTimeTooShortException.class, () -> {
            aiFeedbackService.getFeedback(request);
        });

        assertTrue(exception.getMessage().contains("학습 시간이 너무 짧습니다"));
        assertTrue(exception.getMessage().contains("100초") || exception.getMessage().contains("1분"));
        assertTrue(exception.getMessage().contains("120초") || exception.getMessage().contains("2분"));
        verify(timerRepository, times(1)).findById(1L);
        verify(rateLimiterService, never()).canMakeRequest();
    }

    @Test
    @DisplayName("Rate Limit 초과 시 예외 발생")
    void getFeedback_RateLimitExceeded_ThrowsException() {
        // Given
        when(timerRepository.findById(1L)).thenReturn(Optional.of(timer));
        when(rateLimiterService.canMakeRequest()).thenReturn(false);
        when(rateLimiterService.getCurrentRequestCount()).thenReturn(20);
        when(rateLimiterService.getMaxRequestsPerMinute()).thenReturn(20);

        // When & Then
        RateLimitExceededException exception = assertThrows(RateLimitExceededException.class, () -> {
            aiFeedbackService.getFeedback(request);
        });

        assertTrue(exception.getMessage().contains("사용량") || exception.getMessage().contains("초과") || 
                   exception.getMessage().contains("Rate limit"));
        verify(rateLimiterService, times(1)).canMakeRequest();
    }

    @Test
    @DisplayName("기존 피드백 조회 성공")
    void getExistingFeedback_Success() {
        // Given
        timer.setAiFeedback("피드백 내용");
        timer.setAiSuggestions("제안 내용");
        timer.setAiMotivation("동기부여 메시지");
        when(timerRepository.findById(1L)).thenReturn(Optional.of(timer));

        // When
        AiFeedbackResponse response = aiFeedbackService.getExistingFeedback(1L);

        // Then
        assertNotNull(response);
        assertEquals("피드백 내용", response.getFeedback());
        assertEquals("제안 내용", response.getSuggestions());
        assertEquals("동기부여 메시지", response.getMotivation());
        verify(timerRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("기존 피드백이 없으면 예외 발생")
    void getExistingFeedback_NoFeedback_ThrowsException() {
        // Given
        timer.setAiFeedback(null);
        when(timerRepository.findById(1L)).thenReturn(Optional.of(timer));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            aiFeedbackService.getExistingFeedback(1L);
        });

        assertTrue(exception.getMessage().contains("AI 피드백이 아직 생성되지 않았습니다"));
    }
}

