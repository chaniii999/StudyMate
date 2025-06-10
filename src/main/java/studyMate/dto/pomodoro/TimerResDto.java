package studyMate.dto.pomodoro;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TimerResDto {
    private boolean success;
    private String message;
    private String status;  // STARTED, STOPPED, PAUSED
    private int remainingTime;
    private String userNickname;
} 