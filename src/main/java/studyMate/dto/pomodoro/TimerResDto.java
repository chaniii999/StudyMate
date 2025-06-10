package studyMate.dto.pomodoro;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TimerResDto {
    private boolean success;
    private String message;
    private String status;  // STARTED, STOPPED, PAUSED
    private int remainingTime;  // 남은 시간(초)
    private String timerType;   // STUDY 또는 BREAK
    private String userNickname;
    private int studyMinutes;   // 설정된 공부 시간
    private int breakMinutes;   // 설정된 휴식 시간
    private int cycleCount;     // 현재 사이클 수
} 