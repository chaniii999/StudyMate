package studyMate.dto.timer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyStatsDto {
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private int totalStudyMinutes;
    private List<DailyStatsDto> dailyStats;
    private double averageStudyTimePerDay;
}
