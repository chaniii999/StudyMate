package studyMate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studyMate.dto.studygoal.StudyGoalRequest;
import studyMate.dto.studygoal.StudyGoalResponse;
import studyMate.dto.studygoal.StudyGoalStatistics;
import studyMate.entity.GoalStatus;
import studyMate.entity.StudyGoal;
import studyMate.entity.Timer;
import studyMate.entity.User;
import studyMate.exception.StudyGoalNotFoundException;
import studyMate.repository.StudyGoalRepository;
import studyMate.repository.TimerRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyGoalService {
    
    private final StudyGoalRepository studyGoalRepository;
    private final TimerRepository timerRepository;
    
    // 사용자의 모든 학습목표 조회
    public List<StudyGoalResponse> getAllStudyGoals(User user) {
        List<StudyGoal> studyGoals = studyGoalRepository.findByUserOrderByCreatedAtDesc(user);
        return studyGoals.stream()
                .map(StudyGoalResponse::from)
                .collect(Collectors.toList());
    }
    
    // 사용자의 활성 학습목표 조회
    public List<StudyGoalResponse> getActiveStudyGoals(User user) {
        List<StudyGoal> activeGoals = studyGoalRepository.findByUserAndStatusOrderByCreatedAtDesc(user, GoalStatus.ACTIVE);
        return activeGoals.stream()
                .map(StudyGoalResponse::from)
                .collect(Collectors.toList());
    }
    
    // 특정 학습목표 조회
    public StudyGoalResponse getStudyGoal(User user, Long goalId) {
        StudyGoal studyGoal = studyGoalRepository.findByIdAndUser(goalId, user)
                .orElseThrow(() -> new StudyGoalNotFoundException(goalId));
        return StudyGoalResponse.from(studyGoal);
    }
    
    // 새로운 학습목표 생성
    @Transactional
    public StudyGoalResponse createStudyGoal(User user, StudyGoalRequest request) {
        // 기본값 설정
        if (request.getStatus() == null) {
            request.setStatus(GoalStatus.ACTIVE);
        }
        if (request.getStartDate() == null) {
            request.setStartDate(LocalDate.now());
        }
        if (request.getColor() == null) {
            request.setColor("#3B82F6"); // 기본 파란색
        }
        
        StudyGoal studyGoal = StudyGoal.builder()
                .title(request.getTitle())
                .subject(request.getSubject())
                .description(request.getDescription())
                .color(request.getColor())
                .startDate(request.getStartDate())
                .targetDate(request.getTargetDate())
                .targetHours(request.getTargetHours())
                .targetSessions(request.getTargetSessions())
                .status(request.getStatus())
                .user(user)
                .build();
        
        StudyGoal savedGoal = studyGoalRepository.save(studyGoal);
        log.info("새로운 학습목표 생성: {} (사용자: {})", savedGoal.getTitle(), user.getEmail());
        
        return StudyGoalResponse.from(savedGoal);
    }
    
    // 학습목표 수정
    @Transactional
    public StudyGoalResponse updateStudyGoal(User user, Long goalId, StudyGoalRequest request) {
        StudyGoal studyGoal = studyGoalRepository.findByIdAndUser(goalId, user)
                .orElseThrow(() -> new StudyGoalNotFoundException(goalId));
        
        // 학습목표 정보 업데이트 (엔티티 메서드 사용)
        studyGoal.updateFromRequest(request);
        
        StudyGoal updatedGoal = studyGoalRepository.save(studyGoal);
        log.info("학습목표 수정: {} (사용자: {})", updatedGoal.getTitle(), user.getEmail());
        
        return StudyGoalResponse.from(updatedGoal);
    }
    
    // 학습목표 삭제
    @Transactional
    public void deleteStudyGoal(User user, Long goalId) {
        StudyGoal studyGoal = studyGoalRepository.findByIdAndUser(goalId, user)
                .orElseThrow(() -> new StudyGoalNotFoundException(goalId));
        
        studyGoalRepository.delete(studyGoal);
        log.info("학습목표 삭제: {} (사용자: {})", studyGoal.getTitle(), user.getEmail());
    }
    
    // 학습목표 진행도 업데이트 (타이머 완료 시 호출)
    @Transactional
    public void updateProgress(Long goalId, int studyMinutes) {
        StudyGoal studyGoal = studyGoalRepository.findById(goalId)
                .orElseThrow(() -> new StudyGoalNotFoundException(goalId));
        
        // 진행 시간 업데이트 (분 단위로 정확하게 누적)
        int newCurrentMinutes = studyGoal.getCurrentMinutes() + studyMinutes;
        int newCurrentHours = newCurrentMinutes / 60; // 시간으로 변환
        
        studyGoal.setCurrentMinutes(newCurrentMinutes);
        studyGoal.setCurrentHours(newCurrentHours);
        
        // 세션 수 증가
        studyGoal.setCurrentSessions(studyGoal.getCurrentSessions() + 1);
        
        // 목표 달성 확인 (분 단위까지 고려)
        int targetTotalMinutes = studyGoal.getTargetHours() * 60;
        if (newCurrentMinutes >= targetTotalMinutes) {
            studyGoal.setStatus(GoalStatus.COMPLETED);
            log.info("학습목표 달성: {} (사용자: {}) - 총 {}분 달성", 
                    studyGoal.getTitle(), studyGoal.getUser().getEmail(), newCurrentMinutes);
        }
        
        log.info("학습목표 진행도 업데이트: {} - +{}분 (총 {}분, {}시간)", 
                studyGoal.getTitle(), studyMinutes, newCurrentMinutes, newCurrentHours);
        
        studyGoalRepository.save(studyGoal);
    }
    
    // 학습목표별 통계 조회
    public StudyGoalStatistics getStudyGoalStatistics(User user, Long goalId, 
                                                     LocalDate startDate, LocalDate endDate) {
        StudyGoal studyGoal = studyGoalRepository.findByIdAndUser(goalId, user)
                .orElseThrow(() -> new StudyGoalNotFoundException(goalId));
        
        // 해당 기간의 타이머 기록 조회 (특정 학습목표별로 필터링)
        List<Timer> timerRecords = timerRepository.findByUserAndOptionalStudyGoalAndDateRange(
                user, studyGoal, startDate, endDate);
        
        // 해당 학습목표에 속한 타이머 기록만 필터링
        timerRecords = timerRecords.stream()
                .filter(timer -> timer.getStudyGoal() != null && timer.getStudyGoal().getId().equals(goalId))
                .collect(Collectors.toList());
        
        // 통계 계산
        int totalStudyMinutes = timerRecords.stream()
                .mapToInt(Timer::getStudyTime)
                .sum() / 60; // 초 -> 분 변환
        
        int totalSessions = timerRecords.size();
        
        double averageSessionTime = totalSessions > 0 ? 
                (double) totalStudyMinutes / totalSessions : 0.0;
        
        // 일별 데이터 생성
        List<StudyGoalStatistics.DailyStudyData> dailyData = timerRecords.stream()
                .collect(Collectors.groupingBy(timer -> timer.getCreatedAt().toLocalDate()))
                .entrySet().stream()
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<Timer> dayTimers = entry.getValue();
                    int dayStudyMinutes = dayTimers.stream()
                            .mapToInt(Timer::getStudyTime)
                            .sum() / 60;
                    
                    return StudyGoalStatistics.DailyStudyData.builder()
                            .date(date)
                            .studyMinutes(dayStudyMinutes)
                            .sessions(dayTimers.size())
                            .dayOfWeek(date.getDayOfWeek().name())
                            .build();
                })
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .collect(Collectors.toList());
        
        // 주간별 데이터
        Map<String, Integer> weeklyData = timerRecords.stream()
                .collect(Collectors.groupingBy(
                        timer -> timer.getCreatedAt().toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-'W'ww")),
                        Collectors.summingInt(timer -> timer.getStudyTime() / 60)
                ));
        
        return StudyGoalStatistics.builder()
                .goalId(studyGoal.getId())
                .title(studyGoal.getTitle())
                .subject(studyGoal.getSubject())
                .color(studyGoal.getColor())
                .totalStudyHours(studyGoal.getCurrentHours())
                .totalStudySessions(studyGoal.getCurrentSessions())
                .averageSessionTime(averageSessionTime)
                .progressRate(studyGoal.getProgressRate())
                .studyHoursInPeriod(totalStudyMinutes / 60)
                .studySessionsInPeriod(totalSessions)
                .dailyData(dailyData)
                .weeklyData(weeklyData)
                .build();
    }
}