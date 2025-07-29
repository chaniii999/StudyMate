package studyMate.dto.timer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyStatsDto {
    private LocalDate date;
    private int totalStudyMinutes;
    private int totalRestMinutes;
    private int sessionCount;
    private int longestSessionMinutes;
}
