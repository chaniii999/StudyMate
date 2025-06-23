package studyMate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studyMate.dto.pomodoro.TimerReqDto;
import studyMate.dto.pomodoro.TimerResDto;
import studyMate.entity.User;
import studyMate.entity.Timer;
import studyMate.repository.UserRepository;
import studyMate.repository.TimerRepository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimerService {

    private final UserRepository userRepository;
    private final TimerRepository timerRepository;
    
    // 사용자별 타이머 상태를 저장하는 맵
    private final Map<String, TimerStatus> userTimerStatus = new ConcurrentHashMap<>();

    @Transactional
    public TimerResDto startTimer(User user, TimerReqDto dto) {
        String timerType = dto.getTimerType() != null ? dto.getTimerType() : "STUDY";
        
        // 현재 사용자의 타이머 상태 저장 또는 업데이트
        TimerStatus status = userTimerStatus.getOrDefault(user.getId(), 
            new TimerStatus(dto.getStudyMinutes(), dto.getBreakMinutes()));
        
        status.setStatus("STARTED");
        status.setTimerType(timerType);
        status.setCurrentDuration(status.calculateNewDuration());
        status.setStartTime(System.currentTimeMillis());
        
        userTimerStatus.put(user.getId(), status);

        return buildTimerResponse(true, 
            status.isStudyMode() ? "학습 타이머가 시작되었습니다." : "휴식 타이머가 시작되었습니다.",
            status, user);
    }

    @Transactional
    public TimerResDto stopTimer(User user) {
        TimerStatus status = userTimerStatus.remove(user.getId());
        if (status == null) {
            return buildErrorResponse("실행 중인 타이머가 없습니다.");
        }

        return buildTimerResponse(true, "타이머가 중지되었습니다.", status, user);
    }

    @Transactional
    public TimerResDto pauseTimer(User user) {
        TimerStatus status = userTimerStatus.get(user.getId());
        if (status == null) {
            return buildErrorResponse("실행 중인 타이머가 없습니다.");
        }

        status.setStatus("PAUSED");
        status.setRemainingTime(status.calculateRemainingTime());
        userTimerStatus.put(user.getId(), status);

        return buildTimerResponse(true, "타이머가 일시정지되었습니다.", status, user);
    }

    @Transactional
    public TimerResDto switchTimer(User user) {
        TimerStatus status = userTimerStatus.get(user.getId());
        if (status == null) {
            return buildErrorResponse("실행 중인 타이머가 없습니다.");
        }

        status.updateForNewCycle();
        userTimerStatus.put(user.getId(), status);

        return buildTimerResponse(true, 
            status.isStudyMode() ? "학습 타이머가 시작되었습니다." : "휴식 타이머가 시작되었습니다.",
            status, user);
    }

    @Transactional
    public Timer saveTimerRecord(User user, int studyMinutes, int restMinutes, LocalDateTime startTime, LocalDateTime endTime, String mode, String summary) {
        Timer timer = Timer.builder()
                .user(user)
                .studyMinutes(studyMinutes)
                .restMinutes(restMinutes)
                .startTime(startTime)
                .endTime(endTime)
                .mode(mode)
                .summary(summary)
                .build();
        return timerRepository.save(timer);
    }

    private TimerResDto buildErrorResponse(String message) {
        return TimerResDto.builder()
                .success(false)
                .message(message)
                .status("STOPPED")
                .remainingTime(0)
                .build();
    }

    private TimerResDto buildTimerResponse(boolean success, String message, TimerStatus status, User user) {
        return TimerResDto.builder()
                .success(success)
                .message(message)
                .status(status.getStatus())
                .remainingTime(status.calculateRemainingTime())
                .timerType(status.getTimerType())
                .userNickname(user.getNickname())
                .studyMinutes(status.getStudyMinutes())
                .breakMinutes(status.getBreakMinutes())
                .cycleCount(status.getCycleCount())
                .build();
    }
}
