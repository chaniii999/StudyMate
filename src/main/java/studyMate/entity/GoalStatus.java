package studyMate.entity;

public enum GoalStatus {
    ACTIVE("진행 중"),
    COMPLETED("완료"),
    PAUSED("일시정지"),
    CANCELLED("취소");
    
    private final String description;
    
    GoalStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}