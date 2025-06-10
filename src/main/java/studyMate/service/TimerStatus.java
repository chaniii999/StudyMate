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
        switchMode();
        this.currentDuration = calculateNewDuration();
        this.startTime = System.currentTimeMillis();
        this.status = "STARTED";
    }

    public int calculateRemainingTime() {
        if ("STOPPED".equals(status)) return 0;
        long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
        return (int) Math.max(0, currentDuration - elapsedTime);
    }
} 