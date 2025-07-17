import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import studyMate.dto.pomodoro.TimerReqDto;
import studyMate.dto.pomodoro.TimerResDto;
import studyMate.entity.User;
import studyMate.repository.TimerRepository;
import studyMate.repository.UserRepository;
import studyMate.service.TimerService;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TimerServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TimerRepository timerRepository;

    private TimerService timerService;
    private User user;

    @BeforeEach
    void setUp() {
        timerService = new TimerService(userRepository, timerRepository);
        user = User.builder()
                .id("user1")
                .email("test@example.com")
                .password("pwd")
                .nickname("tester")
                .sex("M")
                .build();
    }

    @Test
    void startTimer_startsStudyTimer() {
        TimerReqDto dto = new TimerReqDto();
        dto.setStudyTimes(25);
        dto.setBreakTimes(5);

        TimerResDto res = timerService.startTimer(user, dto);

        assertTrue(res.isSuccess());
        assertEquals("STARTED", res.getStatus());
        assertEquals("STUDY", res.getTimerType());
        assertEquals("tester", res.getUserNickname());
        assertTrue(res.getRemainingTime() <= 25 * 60);
    }

    @Test
    void pauseTimer_pausesRunningTimer() throws InterruptedException {
        TimerReqDto dto = new TimerReqDto();
        dto.setStudyTimes(25);
        dto.setBreakTimes(5);
        timerService.startTimer(user, dto);

        Thread.sleep(10); // ensure some time passes
        TimerResDto res = timerService.pauseTimer(user);

        assertTrue(res.isSuccess());
        assertEquals("PAUSED", res.getStatus());
        assertTrue(res.getRemainingTime() < 25 * 60);
    }

    @Test
    void stopTimer_stopsRunningTimer() {
        TimerReqDto dto = new TimerReqDto();
        dto.setStudyTimes(25);
        dto.setBreakTimes(5);
        timerService.startTimer(user, dto);

        TimerResDto res = timerService.stopTimer(user);
        assertTrue(res.isSuccess());

        TimerResDto res2 = timerService.stopTimer(user);
        assertFalse(res2.isSuccess());
        assertEquals("실행 중인 타이머가 없습니다.", res2.getMessage());
    }
}
