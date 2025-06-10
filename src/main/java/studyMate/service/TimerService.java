package studyMate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studyMate.dto.pomodoro.TimerReqDto;
import studyMate.dto.pomodoro.TimerResDto;
import studyMate.entity.User;
import studyMate.repository.UserRepository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimerService {

    private final UserRepository userRepository;
    
    // 사용자별 타이머 상태를 저장하는 맵 (임시 저장소로 사용)
    private final Map<String, TimerStatus> userTimerStatus = new ConcurrentHashMap<>();

    @Transactional
    public TimerResDto startTimer(User user, TimerReqDto dto) {
        String timerType = dto.getTimerType() != null ? dto.getTimerType() : "STUDY";
        int duration = "STUDY".equals(timerType) ? 
            dto.getStudyMinutes() * 60 : dto.getBreakMinutes() * 60;

        // 현재 사용자의 타이머 상태 저장 또는 업데이트
        TimerStatus status = userTimerStatus.getOrDefault(user.getId(), 
            new TimerStatus(dto.getStudyMinutes(), dto.getBreakMinutes()));
        
        status.setStatus("STARTED");
        status.setTimerType(timerType);
        status.setCurrentDuration(duration);
        status.setStartTime(System.currentTimeMillis());
        
        userTimerStatus.put(user.getId(), status);

        return TimerResDto.builder()
                .success(true)
                .message(timerType.equals("STUDY") ? "학습 타이머가 시작되었습니다." : "휴식 타이머가 시작되었습니다.")
                .status("STARTED")
                .remainingTime(duration)
                .timerType(timerType)
                .userNickname(user.getNickname())
                .studyMinutes(dto.getStudyMinutes())
                .breakMinutes(dto.getBreakMinutes())
                .cycleCount(status.getCycleCount())
                .build();
    }

    @Transactional
    public TimerResDto stopTimer(User user) {
        TimerStatus status = userTimerStatus.remove(user.getId());
        if (status == null) {
            return TimerResDto.builder()
                    .success(false)
                    .message("실행 중인 타이머가 없습니다.")
                    .status("STOPPED")
                    .remainingTime(0)
                    .build();
        }

        return TimerResDto.builder()
                .success(true)
                .message("타이머가 중지되었습니다.")
                .status("STOPPED")
                .remainingTime(0)
                .timerType(status.getTimerType())
                .userNickname(user.getNickname())
                .studyMinutes(status.getStudyMinutes())
                .breakMinutes(status.getBreakMinutes())
                .cycleCount(status.getCycleCount())
                .build();
    }

    @Transactional
    public TimerResDto pauseTimer(User user) {
        TimerStatus status = userTimerStatus.get(user.getId());
        if (status == null) {
            return TimerResDto.builder()
                    .success(false)
                    .message("실행 중인 타이머가 없습니다.")
                    .status("STOPPED")
                    .remainingTime(0)
                    .build();
        }

        // 남은 시간 계산
        long elapsedTime = (System.currentTimeMillis() - status.getStartTime()) / 1000;
        int remainingTime = (int) Math.max(0, status.getCurrentDuration() - elapsedTime);

        // 상태 업데이트
        status.setStatus("PAUSED");
        status.setRemainingTime(remainingTime);
        userTimerStatus.put(user.getId(), status);

        return TimerResDto.builder()
                .success(true)
                .message("타이머가 일시정지되었습니다.")
                .status("PAUSED")
                .remainingTime(remainingTime)
                .timerType(status.getTimerType())
                .userNickname(user.getNickname())
                .studyMinutes(status.getStudyMinutes())
                .breakMinutes(status.getBreakMinutes())
                .cycleCount(status.getCycleCount())
                .build();
    }

    @Transactional
    public TimerResDto switchTimer(User user) {
        TimerStatus status = userTimerStatus.get(user.getId());
        if (status == null) {
            return TimerResDto.builder()
                    .success(false)
                    .message("실행 중인 타이머가 없습니다.")
                    .status("STOPPED")
                    .remainingTime(0)
                    .build();
        }

        // 타이머 타입 전환
        String newTimerType = "STUDY".equals(status.getTimerType()) ? "BREAK" : "STUDY";
        int newDuration = "STUDY".equals(newTimerType) ? 
            status.getStudyMinutes() * 60 : status.getBreakMinutes() * 60;

        // BREAK -> STUDY로 전환될 때 사이클 카운트 증가
        if ("STUDY".equals(newTimerType)) {
            status.incrementCycleCount();
        }

        status.setTimerType(newTimerType);
        status.setCurrentDuration(newDuration);
        status.setStartTime(System.currentTimeMillis());
        status.setStatus("STARTED");

        userTimerStatus.put(user.getId(), status);

        return TimerResDto.builder()
                .success(true)
                .message(newTimerType.equals("STUDY") ? "학습 타이머가 시작되었습니다." : "휴식 타이머가 시작되었습니다.")
                .status("STARTED")
                .remainingTime(newDuration)
                .timerType(newTimerType)
                .userNickname(user.getNickname())
                .studyMinutes(status.getStudyMinutes())
                .breakMinutes(status.getBreakMinutes())
                .cycleCount(status.getCycleCount())
                .build();
    }

    // 타이머 상태를 저장하기 위한 내부 클래스
    private static class TimerStatus {
        private String status;
        private String timerType;
        private int studyMinutes;
        private int breakMinutes;
        private int currentDuration;
        private int remainingTime;
        private long startTime;
        private int cycleCount;

        public TimerStatus(int studyMinutes, int breakMinutes) {
            this.status = "STOPPED";
            this.timerType = "STUDY";
            this.studyMinutes = studyMinutes;
            this.breakMinutes = breakMinutes;
            this.cycleCount = 0;
        }

        public void incrementCycleCount() {
            this.cycleCount++;
        }

        // Getters and Setters
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getTimerType() { return timerType; }
        public void setTimerType(String timerType) { this.timerType = timerType; }
        public int getStudyMinutes() { return studyMinutes; }
        public int getBreakMinutes() { return breakMinutes; }
        public int getCurrentDuration() { return currentDuration; }
        public void setCurrentDuration(int currentDuration) { this.currentDuration = currentDuration; }
        public int getRemainingTime() { return remainingTime; }
        public void setRemainingTime(int remainingTime) { this.remainingTime = remainingTime; }
        public long getStartTime() { return startTime; }
        public void setStartTime(long startTime) { this.startTime = startTime; }
        public int getCycleCount() { return cycleCount; }
    }
}
