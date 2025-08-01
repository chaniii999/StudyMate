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
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.util.List;

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
            new TimerStatus(dto.getStudyTimes(), dto.getBreakTimes()));
        
        status.setStatus("STARTED");
        status.setTimerType(timerType);
        status.setCurrentDuration(status.calculateNewDuration());
        status.startSession(); // 세션 시작
        
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

        // 세션 종료 시 실제 경과 시간 업데이트
        status.endSession();
        
        log.info("타이머 중지 - 사용자: {}, 실제 학습시간: {}분, 실제 휴식시간: {}분", 
                user.getNickname(), status.getActualStudyMinutes(), status.getActualRestMinutes());

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
    public Timer saveTimerRecord(User user, int studySeconds, int restSeconds, LocalDateTime startTime, LocalDateTime endTime, String mode, String summary) {
        // 실제 경과 시간 계산 (초 단위)
        int actualStudySeconds = studySeconds;
        int actualRestSeconds = restSeconds;
        
        if (startTime != null && endTime != null) {
            long totalSeconds = java.time.Duration.between(startTime, endTime).getSeconds();
            
            // 모드에 따라 학습/휴식 시간 분배
            if ("STUDY".equals(mode) || mode == null) {
                actualStudySeconds = (int) totalSeconds;
                actualRestSeconds = 0;
            } else if ("BREAK".equals(mode)) {
                actualStudySeconds = 0;
                actualRestSeconds = (int) totalSeconds;
            }
            // 포모도로 모드 등에서는 전달받은 초 단위 값을 그대로 사용
        }
        
        Timer timer = Timer.builder()
                .user(user)
                .studyTime(actualStudySeconds) // 초 단위로 저장
                .restTime(actualRestSeconds)   // 초 단위로 저장
                .startTime(startTime)
                .endTime(endTime)
                .mode(mode)
                .summary(summary)
                .build();
        
        log.info("타이머 기록 저장 - 사용자: {}, 실제 학습시간: {}초({}분), 실제 휴식시간: {}초({}분), 총 경과시간: {}분", 
                user.getNickname(), actualStudySeconds, actualStudySeconds/60, actualRestSeconds, actualRestSeconds/60,
                startTime != null && endTime != null ? java.time.Duration.between(startTime, endTime).toMinutes() : 0);
        
        return timerRepository.save(timer);
    }

    /**
     * 타이머 기록 삭제
     */
    @Transactional
    public boolean deleteTimerRecord(User user, Long timerId) {
        log.info("타이머 기록 삭제 요청 - 사용자: {}, 타이머 ID: {}", user.getNickname(), timerId);
        
        // 타이머 기록 조회
        Timer timer = timerRepository.findById(timerId).orElse(null);
        if (timer == null) {
            log.warn("타이머 기록을 찾을 수 없습니다 - ID: {}", timerId);
            return false;
        }
        
        // 권한 확인 (본인의 기록만 삭제 가능)
        if (!timer.getUser().getId().equals(user.getId())) {
            log.warn("권한 없는 타이머 기록 삭제 시도 - 사용자: {}, 기록 소유자: {}, 타이머 ID: {}", 
                    user.getId(), timer.getUser().getId(), timerId);
            return false;
        }
        
        // 삭제 수행
        timerRepository.delete(timer);
        log.info("타이머 기록 삭제 완료 - 사용자: {}, 타이머 ID: {}", user.getNickname(), timerId);
        
        return true;
    }

    /**
     * 홈 화면 통계 조회
     */
    public Map<String, Object> getHomeStats(User user) {
        Map<String, Object> stats = new HashMap<>();
        
        // 오늘 날짜
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);
        
        // 이번 주 시작 (월요일)
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDateTime startOfWeekTime = startOfWeek.atStartOfDay();
        LocalDateTime endOfWeekTime = today.atTime(23, 59, 59);
        
        // 오늘 공부 시간 계산
        List<Timer> todayTimers = timerRepository.findByUserAndStartTimeBetween(user, startOfDay, endOfDay);
        int todayStudySeconds = todayTimers.stream()
                .mapToInt(Timer::getStudyTime)
                .sum();
        
        // 이번 주 공부 시간 계산
        List<Timer> weekTimers = timerRepository.findByUserAndStartTimeBetween(user, startOfWeekTime, endOfWeekTime);
        int weekStudySeconds = weekTimers.stream()
                .mapToInt(Timer::getStudyTime)
                .sum();
        
        log.info("홈 통계 계산 - 사용자: {}, 오늘: {}초({}분), 이번주: {}초({}분)", 
                user.getNickname(), todayStudySeconds, todayStudySeconds/60, weekStudySeconds, weekStudySeconds/60);
        
        stats.put("todayStudyMinutes", todayStudySeconds / 60);
        stats.put("weekStudyMinutes", weekStudySeconds / 60);
        stats.put("todayStudySeconds", todayStudySeconds);
        stats.put("weekStudySeconds", weekStudySeconds);
        
        return stats;
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
                .studyTimes(status.getStudyMinutes())
                .breakTimes(status.getBreakMinutes())
                .cycleCount(status.getCycleCount())
                .build();
    }
}
