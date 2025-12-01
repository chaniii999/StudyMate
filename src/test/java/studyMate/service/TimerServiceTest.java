package studyMate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import studyMate.dto.pomodoro.TimerReqDto;
import studyMate.dto.pomodoro.TimerResDto;
import studyMate.entity.StudyGoal;
import studyMate.entity.Timer;
import studyMate.entity.User;
import studyMate.exception.StudyGoalNotFoundException;
import studyMate.repository.StudyGoalRepository;
import studyMate.repository.TimerRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TimerService 테스트")
class TimerServiceTest {

    @Mock
    private TimerRepository timerRepository;

    @Mock
    private StudyGoalRepository studyGoalRepository;

    @Mock
    private StudyGoalService studyGoalService;

    @InjectMocks
    private TimerService timerService;

    private User user;
    private StudyGoal studyGoal;
    private Timer timer;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id("user1")
                .email("test@example.com")
                .password("pwd")
                .nickname("tester")
                .sex("M")
                .build();

        studyGoal = StudyGoal.builder()
                .id(1L)
                .user(user)
                .title("Java 학습")
                .targetHours(100)
                .build();

        timer = Timer.builder()
                .id(1L)
                .user(user)
                .studyTime(1800) // 30분 (초 단위)
                .restTime(300) // 5분
                .mode("STUDY")
                .startTime(LocalDateTime.now().minusMinutes(30))
                .endTime(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("타이머 시작 성공")
    void startTimer_Success() {
        // Given
        TimerReqDto dto = new TimerReqDto();
        dto.setStudyTimes(25);
        dto.setBreakTimes(5);

        // When
        TimerResDto res = timerService.startTimer(user, dto);

        // Then
        assertTrue(res.isSuccess());
        assertEquals("STARTED", res.getStatus());
        assertEquals("STUDY", res.getTimerType());
        assertEquals("tester", res.getUserNickname());
        assertTrue(res.getRemainingTime() <= 25 * 60);
    }

    @Test
    @DisplayName("타이머 일시정지 성공")
    void pauseTimer_Success() {
        // Given
        TimerReqDto dto = new TimerReqDto();
        dto.setStudyTimes(25);
        dto.setBreakTimes(5);
        timerService.startTimer(user, dto);

        // When
        TimerResDto res = timerService.pauseTimer(user);

        // Then
        assertTrue(res.isSuccess());
        assertEquals("PAUSED", res.getStatus());
        assertTrue(res.getRemainingTime() >= 0);
    }

    @Test
    @DisplayName("실행 중인 타이머 없이 일시정지 시 실패")
    void pauseTimer_NoRunningTimer_ReturnsError() {
        // When
        TimerResDto res = timerService.pauseTimer(user);

        // Then
        assertFalse(res.isSuccess());
        assertEquals("실행 중인 타이머가 없습니다.", res.getMessage());
        assertEquals("STOPPED", res.getStatus());
    }

    @Test
    @DisplayName("타이머 중지 성공")
    void stopTimer_Success() {
        // Given
        TimerReqDto dto = new TimerReqDto();
        dto.setStudyTimes(25);
        dto.setBreakTimes(5);
        timerService.startTimer(user, dto);

        // When
        TimerResDto res = timerService.stopTimer(user);

        // Then
        assertTrue(res.isSuccess());
        assertEquals("타이머가 중지되었습니다.", res.getMessage());
    }

    @Test
    @DisplayName("실행 중인 타이머 없이 중지 시 실패")
    void stopTimer_NoRunningTimer_ReturnsError() {
        // When
        TimerResDto res = timerService.stopTimer(user);

        // Then
        assertFalse(res.isSuccess());
        assertEquals("실행 중인 타이머가 없습니다.", res.getMessage());
    }

    @Test
    @DisplayName("타이머 전환 성공 (학습 -> 휴식)")
    void switchTimer_Success() {
        // Given
        TimerReqDto dto = new TimerReqDto();
        dto.setStudyTimes(25);
        dto.setBreakTimes(5);
        timerService.startTimer(user, dto);

        // When
        TimerResDto res = timerService.switchTimer(user);

        // Then
        assertTrue(res.isSuccess());
        assertTrue(res.getMessage().contains("타이머가 시작되었습니다"));
    }

    @Test
    @DisplayName("실행 중인 타이머 없이 전환 시 실패")
    void switchTimer_NoRunningTimer_ReturnsError() {
        // When
        TimerResDto res = timerService.switchTimer(user);

        // Then
        assertFalse(res.isSuccess());
        assertEquals("실행 중인 타이머가 없습니다.", res.getMessage());
    }

    @Test
    @DisplayName("타이머 기록 저장 성공 (학습목표 없음)")
    void saveTimerRecord_WithoutStudyGoal_Success() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(30);
        LocalDateTime endTime = LocalDateTime.now();
        when(timerRepository.save(any(Timer.class))).thenReturn(timer);

        // When
        Timer result = timerService.saveTimerRecord(user, 30, 5, startTime, endTime, "STUDY", "학습 요약");

        // Then
        assertNotNull(result);
        verify(timerRepository, times(1)).save(any(Timer.class));
        verify(studyGoalService, never()).updateProgress(anyLong(), anyInt());
    }

    @Test
    @DisplayName("타이머 기록 저장 성공 (학습목표 연동)")
    void saveTimerRecord_WithStudyGoal_Success() {
        // Given
        timer.setStudyGoal(studyGoal);
        when(studyGoalRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(studyGoal));
        when(timerRepository.save(any(Timer.class))).thenReturn(timer);
        doNothing().when(studyGoalService).updateProgress(1L, 30);

        // When
        Timer result = timerService.saveTimerRecord(user, timer, 1L);

        // Then
        assertNotNull(result);
        verify(studyGoalRepository, times(1)).findByIdAndUser(1L, user);
        verify(studyGoalService, times(1)).updateProgress(1L, 30);
        verify(timerRepository, times(1)).save(any(Timer.class));
    }

    @Test
    @DisplayName("존재하지 않는 학습목표로 타이머 저장 시 예외 발생")
    void saveTimerRecord_StudyGoalNotFound_ThrowsException() {
        // Given
        when(studyGoalRepository.findByIdAndUser(999L, user)).thenReturn(Optional.empty());

        // When & Then
        StudyGoalNotFoundException exception = assertThrows(StudyGoalNotFoundException.class, () -> {
            timerService.saveTimerRecord(user, timer, 999L);
        });

        assertTrue(exception.getMessage().contains("학습목표") && exception.getMessage().contains("999"));
        verify(timerRepository, never()).save(any(Timer.class));
    }

    @Test
    @DisplayName("타이머 기록 삭제 성공")
    void deleteTimerRecord_Success() {
        // Given
        when(timerRepository.findById(1L)).thenReturn(Optional.of(timer));
        doNothing().when(timerRepository).delete(timer);

        // When
        boolean result = timerService.deleteTimerRecord(user, 1L);

        // Then
        assertTrue(result);
        verify(timerRepository, times(1)).findById(1L);
        verify(timerRepository, times(1)).delete(timer);
    }

    @Test
    @DisplayName("다른 사용자의 타이머 삭제 시 실패")
    void deleteTimerRecord_DifferentUser_ReturnsFalse() {
        // Given
        User otherUser = User.builder().id("user2").build();
        when(timerRepository.findById(1L)).thenReturn(Optional.of(timer));

        // When
        boolean result = timerService.deleteTimerRecord(otherUser, 1L);

        // Then
        assertFalse(result);
        verify(timerRepository, never()).delete(any(Timer.class));
    }

    @Test
    @DisplayName("학습목표 연동된 타이머 삭제 시 진행도 차감")
    void deleteTimerRecord_WithStudyGoal_DeductsProgress() {
        // Given
        timer.setStudyGoal(studyGoal);
        studyGoal.setCurrentHours(10);
        studyGoal.setCurrentSessions(5);
        when(timerRepository.findById(1L)).thenReturn(Optional.of(timer));
        when(studyGoalRepository.save(any(StudyGoal.class))).thenReturn(studyGoal);
        doNothing().when(timerRepository).delete(timer);

        // When
        boolean result = timerService.deleteTimerRecord(user, 1L);

        // Then
        assertTrue(result);
        verify(studyGoalRepository, times(1)).save(any(StudyGoal.class));
        verify(timerRepository, times(1)).delete(timer);
    }

    @Test
    @DisplayName("오늘 학습시간 조회")
    void getTodayStudyTime_Success() {
        // Given
        List<Timer> todayTimers = List.of(timer);
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);
        when(timerRepository.findByUserAndStartTimeBetween(user, startOfDay, endOfDay))
                .thenReturn(todayTimers);

        // When
        int studyTime = timerService.getTodayStudyTime(user);

        // Then
        assertEquals(30, studyTime); // 1800초 = 30분
        verify(timerRepository, times(1)).findByUserAndStartTimeBetween(user, startOfDay, endOfDay);
    }

    @Test
    @DisplayName("이번주 학습시간 조회")
    void getWeekStudyTime_Success() {
        // Given
        List<Timer> weekTimers = List.of(timer);
        when(timerRepository.findByUserAndStartTimeBetween(any(), any(), any()))
                .thenReturn(weekTimers);

        // When
        int studyTime = timerService.getWeekStudyTime(user);

        // Then
        assertEquals(30, studyTime);
        verify(timerRepository, times(1)).findByUserAndStartTimeBetween(any(), any(), any());
    }

    @Test
    @DisplayName("총 학습시간 조회")
    void getTotalStudyTime_Success() {
        // Given
        // 1800초 = 30분이므로, sumStudyTimeByUser는 1800을 반환하고, 이를 60으로 나누면 30분
        when(timerRepository.sumStudyTimeByUser(user)).thenReturn(1800);

        // When
        int totalTime = timerService.getTotalStudyTime(user);

        // Then
        assertEquals(30, totalTime); // 1800초 / 60 = 30분
        verify(timerRepository, times(1)).sumStudyTimeByUser(user);
    }

    @Test
    @DisplayName("총 세션 수 조회")
    void getTotalSessionCount_Success() {
        // Given
        when(timerRepository.countByUser(user)).thenReturn(5);

        // When
        int count = timerService.getTotalSessionCount(user);

        // Then
        assertEquals(5, count);
        verify(timerRepository, times(1)).countByUser(user);
    }

    @Test
    @DisplayName("평균 세션 시간 조회")
    void getAverageSessionTime_Success() {
        // Given
        // 1800초(30분)와 1200초(20분)의 평균 = 1500초, 이를 60으로 나누면 25분
        when(timerRepository.avgStudyTimeByUser(user)).thenReturn(1500.0);

        // When
        double average = timerService.getAverageSessionTime(user);

        // Then
        assertEquals(25.0, average, 0.01); // 1500초 / 60 = 25분 (소수점 오차 허용)
        verify(timerRepository, times(1)).avgStudyTimeByUser(user);
    }

    @Test
    @DisplayName("기록이 없을 때 평균 세션 시간은 0")
    void getAverageSessionTime_NoRecords_ReturnsZero() {
        // Given
        // avgStudyTimeByUser는 DB 집계 쿼리이므로 기록이 없으면 0.0을 반환
        when(timerRepository.avgStudyTimeByUser(user)).thenReturn(0.0);

        // When
        double average = timerService.getAverageSessionTime(user);

        // Then
        assertEquals(0.0, average);
        verify(timerRepository, times(1)).avgStudyTimeByUser(user);
    }

    @Test
    @DisplayName("최장 세션 시간 조회")
    void getLongestSessionTime_Success() {
        // Given
        // 3600초 = 60분이므로, maxStudyTimeByUser는 3600을 반환하고, 이를 60으로 나누면 60분
        when(timerRepository.maxStudyTimeByUser(user)).thenReturn(3600);

        // When
        int longest = timerService.getLongestSessionTime(user);

        // Then
        assertEquals(60, longest); // 3600초 / 60 = 60분
        verify(timerRepository, times(1)).maxStudyTimeByUser(user);
    }

    @Test
    @DisplayName("학습목표별 학습시간 조회")
    void getStudyTimeByGoal_Success() {
        // Given
        timer.setStudyGoal(studyGoal);
        List<Timer> goalTimers = List.of(timer);
        when(studyGoalRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(studyGoal));
        when(timerRepository.findByUserAndStudyGoalOrderByCreatedAtDesc(user, studyGoal)).thenReturn(goalTimers);

        // When
        int studyTime = timerService.getStudyTimeByGoal(user, 1L);

        // Then
        assertEquals(30, studyTime); // 1800초 / 60 = 30분
        verify(studyGoalRepository, times(1)).findByIdAndUser(1L, user);
        verify(timerRepository, times(1)).findByUserAndStudyGoalOrderByCreatedAtDesc(user, studyGoal);
    }

    @Test
    @DisplayName("학습목표별 세션 수 조회")
    void getSessionCountByGoal_Success() {
        // Given
        when(studyGoalRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(studyGoal));
        when(timerRepository.countByUserAndStudyGoal(user, studyGoal)).thenReturn(3);

        // When
        int count = timerService.getSessionCountByGoal(user, 1L);

        // Then
        assertEquals(3, count);
    }

    @Test
    @DisplayName("월별 학습시간 조회")
    void getMonthStudyTime_Success() {
        // Given
        List<Timer> monthTimers = List.of(timer);
        when(timerRepository.findByUserAndStartTimeBetween(any(), any(), any()))
                .thenReturn(monthTimers);

        // When
        int studyTime = timerService.getMonthStudyTime(user, 2024, 1);

        // Then
        assertEquals(30, studyTime);
        verify(timerRepository, times(1)).findByUserAndStartTimeBetween(any(), any(), any());
    }

    @Test
    @DisplayName("연도별 학습시간 조회")
    void getYearStudyTime_Success() {
        // Given
        List<Timer> yearTimers = List.of(timer);
        when(timerRepository.findByUserAndStartTimeBetween(any(), any(), any()))
                .thenReturn(yearTimers);

        // When
        int studyTime = timerService.getYearStudyTime(user, 2024);

        // Then
        assertEquals(30, studyTime);
        verify(timerRepository, times(1)).findByUserAndStartTimeBetween(any(), any(), any());
    }

    @Test
    @DisplayName("타이머 기록 조회")
    void getTimerHistory_Success() {
        // Given
        List<Timer> timers = List.of(timer);
        when(timerRepository.findByUserOrderByStartTimeDesc(user)).thenReturn(timers);

        // When
        List<Timer> result = timerService.getTimerHistory(user);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(timerRepository, times(1)).findByUserOrderByStartTimeDesc(user);
    }

    @Test
    @DisplayName("기간별 타이머 기록 조회")
    void getTimerHistoryByDateRange_Success() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().minusDays(7);
        LocalDateTime endTime = LocalDateTime.now();
        List<Timer> timers = List.of(timer);
        when(timerRepository.findByUserAndStartTimeBetween(user, startTime, endTime))
                .thenReturn(timers);

        // When
        List<Timer> result = timerService.getTimerHistoryByDateRange(user, startTime, endTime);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(timerRepository, times(1)).findByUserAndStartTimeBetween(user, startTime, endTime);
    }

    @Test
    @DisplayName("학습목표별 타이머 기록 조회")
    void getTimerHistoryByStudyGoal_Success() {
        // Given
        List<Timer> timers = List.of(timer);
        when(studyGoalRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(studyGoal));
        when(timerRepository.findByUserAndStudyGoalOrderByCreatedAtDesc(user, studyGoal)).thenReturn(timers);

        // When
        List<Timer> result = timerService.getTimerHistoryByStudyGoal(user, 1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(studyGoalRepository, times(1)).findByIdAndUser(1L, user);
        verify(timerRepository, times(1)).findByUserAndStudyGoalOrderByCreatedAtDesc(user, studyGoal);
    }

    @Test
    @DisplayName("존재하지 않는 학습목표로 조회 시 예외 발생")
    void getTimerHistoryByStudyGoal_NotFound_ThrowsException() {
        // Given
        when(studyGoalRepository.findByIdAndUser(999L, user)).thenReturn(Optional.empty());

        // When & Then
        StudyGoalNotFoundException exception = assertThrows(StudyGoalNotFoundException.class, () -> {
            timerService.getTimerHistoryByStudyGoal(user, 999L);
        });

        assertTrue(exception.getMessage().contains("학습목표") && exception.getMessage().contains("999"));
    }
}
