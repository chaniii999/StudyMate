package studyMate.service;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TimerStatus {
    private String status;
    private String timerType;
    private int studyMinutes;
    private int breakMinutes;
    private int currentDuration;
    private int remainingTime;
    private long startTime;
    private int cycleCount;
    
    // 실제 경과 시간 추적
    private long actualStudyTime; // 실제 학습 시간 (초)
    private long actualRestTime;  // 실제 휴식 시간 (초)
    private long sessionStartTime; // 세션 시작 시간 (밀리초)

    public TimerStatus(int studyMinutes, int breakMinutes) {
        this.status = "STOPPED";
        this.timerType = "STUDY";
        this.studyMinutes = studyMinutes;
        this.breakMinutes = breakMinutes;
        this.cycleCount = 0;
        this.actualStudyTime = 0;
        this.actualRestTime = 0;
        this.sessionStartTime = 0;
    }

    public void incrementCycleCount() {
        this.cycleCount++;
    }

    public boolean isStudyMode() {
        return "STUDY".equals(this.timerType);
    }

    public void switchMode() {
        this.timerType = isStudyMode() ? "BREAK" : "STUDY";
        if (isStudyMode()) {
            incrementCycleCount();
        }
    }

    public int calculateNewDuration() {
        return (isStudyMode() ? studyMinutes : breakMinutes) * 60;
    }

    public void updateForNewCycle() {
        // 이전 사이클의 실제 경과 시간 업데이트
        updateActualTime();
        
        switchMode();
        this.currentDuration = calculateNewDuration();
        this.startTime = System.currentTimeMillis();
        this.status = "STARTED";
    }
    
    // 실제 경과 시간 업데이트
    public void updateActualTime() {
        if (this.startTime > 0) {
            long elapsedSeconds = (System.currentTimeMillis() - this.startTime) / 1000;
            if (isStudyMode()) {
                this.actualStudyTime += elapsedSeconds;
            } else {
                this.actualRestTime += elapsedSeconds;
            }
        }
    }
    
    // 세션 시작
    public void startSession() {
        this.sessionStartTime = System.currentTimeMillis();
        this.startTime = System.currentTimeMillis();
    }
    
    // 세션 종료 시 최종 시간 업데이트
    public void endSession() {
        updateActualTime();
    }
    
    // 실제 학습 시간 반환 (분)
    public int getActualStudyMinutes() {
        return (int) (actualStudyTime / 60);
    }
    
    // 실제 휴식 시간 반환 (분)
    public int getActualRestMinutes() {
        return (int) (actualRestTime / 60);
    }

    public int calculateRemainingTime() {
        if ("STOPPED".equals(status)) return 0;
        long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
        return (int) Math.max(0, currentDuration - elapsedTime);
    }
} 