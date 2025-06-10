package studyMate.dto.pomodoro;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TimerReqDto {
    private int studyMinutes;  // 공부 시간(분)
    private int breakMinutes;  // 휴식 시간(분)
    private String topicId;    // 학습 주제 ID (선택)
    private String timerType;  // "STUDY" 또는 "BREAK"
}
