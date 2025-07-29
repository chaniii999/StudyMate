package studyMate.dto.timer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeStatsResponseDto {
    private DailyStatsDto today;
    private WeeklyStatsDto thisWeek;
}
