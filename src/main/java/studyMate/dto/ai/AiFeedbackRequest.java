package studyMate.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiFeedbackRequest {
    private Long timerId;
    private String studySummary;
    private int studyTime;
    private int restTime;
    private String mode;
} 