package studyMate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studyMate.entity.StudyGoal;
import studyMate.entity.Timer;
import studyMate.entity.User;
import studyMate.repository.StudyGoalRepository;
import studyMate.repository.TimerRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimerService {
    
    private final TimerRepository timerRepository;
    private final StudyGoalRepository studyGoalRepository;
    private final StudyGoalService studyGoalService;
    
    // 타이머 기록 조회
    public List<Timer> getTimerHistory(User user) {
        return timerRepository.findByUserOrderByStartTimeDesc(user);
    }
    
    // 기간별 타이머 기록 조회
    public List<Timer> getTimerHistoryByDateRange(User user, LocalDateTime startTime, LocalDateTime endTime) {
        return timerRepository.findByUserAndStartTimeBetween(user, startTime, endTime);
    }
    
    // 학습목표별 타이머 기록 조회
    public List<Timer> getTimerHistoryByStudyGoal(User user, Long studyGoalId) {
        StudyGoal studyGoal = studyGoalRepository.findByIdAndUser(studyGoalId, user)
                .orElseThrow(() -> new RuntimeException("학습목표를 찾을 수 없습니다."));
        return timerRepository.findByUserAndStudyGoal(user, studyGoal);
    }
    
    // 타이머 기록 저장 (학습목표 연동)
    @Transactional
    public Timer saveTimerRecord(User user, Timer timer, Long studyGoalId) {
        timer.setUser(user);
        
        // 학습목표가 지정된 경우 연결
        if (studyGoalId != null) {
            StudyGoal studyGoal = studyGoalRepository.findByIdAndUser(studyGoalId, user)
                    .orElseThrow(() -> new RuntimeException("학습목표를 찾을 수 없습니다."));
            timer.setStudyGoal(studyGoal);
            
            // 학습목표 진행도 업데이트
            int studyMinutes = timer.getStudyTime() / 60;
            studyGoalService.updateProgress(studyGoalId, studyMinutes);
            
            log.info("타이머 기록 저장 (학습목표: {}): 학습시간 {}분", 
                    studyGoal.getTitle(), studyMinutes);
        } else {
            log.info("타이머 기록 저장 (학습목표 없음): 학습시간 {}분", timer.getStudyTime() / 60);
        }
        
        return timerRepository.save(timer);
    }
    
    // 타이머 기록 삭제
    @Transactional
    public boolean deleteTimerRecord(User user, Long timerId) {
        return timerRepository.findById(timerId)
                .filter(timer -> timer.getUser().equals(user))
                .map(timer -> {
                    // 학습목표가 연결된 경우 진행도 차감
                    if (timer.getStudyGoal() != null) {
                        StudyGoal studyGoal = timer.getStudyGoal();
                        int studyMinutes = timer.getStudyTime() / 60;
                        int hoursToDeduct = studyMinutes / 60;
                        
                        if (hoursToDeduct > 0) {
                            studyGoal.setCurrentHours(Math.max(0, studyGoal.getCurrentHours() - hoursToDeduct));
                        }
                        studyGoal.setCurrentSessions(Math.max(0, studyGoal.getCurrentSessions() - 1));
                        
                        studyGoalRepository.save(studyGoal);
                        log.info("타이머 기록 삭제로 학습목표 진행도 차감: {} ({}시간, 1세션)", 
                                studyGoal.getTitle(), hoursToDeduct);
                    }
                    
                    timerRepository.delete(timer);
                    log.info("타이머 기록 삭제: {} (사용자: {})", timerId, user.getEmail());
                    return true;
                })
                .orElse(false);
    }
    
    // 사용자의 오늘 학습시간 조회
    public int getTodayStudyTime(User user) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);
        
        List<Timer> todayTimers = timerRepository.findByUserAndStartTimeBetween(user, startOfDay, endOfDay);
        return todayTimers.stream()
                .mapToInt(Timer::getStudyTime)
                .sum() / 60; // 초 -> 분 변환
    }
    
    // 사용자의 이번주 학습시간 조회
    public int getWeekStudyTime(User user) {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDateTime startOfWeekTime = startOfWeek.atStartOfDay();
        LocalDateTime endOfWeekTime = LocalDate.now().atTime(23, 59, 59);
        
        List<Timer> weekTimers = timerRepository.findByUserAndStartTimeBetween(user, startOfWeekTime, endOfWeekTime);
        return weekTimers.stream()
                .mapToInt(Timer::getStudyTime)
                .sum() / 60; // 초 -> 분 변환
    }
    
    // 사용자의 총 학습시간 조회
    public int getTotalStudyTime(User user) {
        List<Timer> allTimers = timerRepository.findByUser(user);
        return allTimers.stream()
                .mapToInt(Timer::getStudyTime)
                .sum() / 60; // 초 -> 분 변환
    }
    
    // 사용자의 총 학습 세션 수 조회
    public int getTotalSessionCount(User user) {
        return timerRepository.countByUser(user);
    }
    
    // 사용자의 평균 세션 시간 조회 (분)
    public double getAverageSessionTime(User user) {
        List<Timer> allTimers = timerRepository.findByUser(user);
        if (allTimers.isEmpty()) {
            return 0.0;
        }
        
        int totalMinutes = allTimers.stream()
                .mapToInt(Timer::getStudyTime)
                .sum() / 60;
        
        return (double) totalMinutes / allTimers.size();
    }
    
    // 최장 학습 세션 시간 조회 (분)
    public int getLongestSessionTime(User user) {
        List<Timer> allTimers = timerRepository.findByUser(user);
        return allTimers.stream()
                .mapToInt(Timer::getStudyTime)
                .max()
                .orElse(0) / 60; // 초 -> 분 변환
    }
    
    // 학습목표별 총 학습시간 조회
    public int getStudyTimeByGoal(User user, Long studyGoalId) {
        StudyGoal studyGoal = studyGoalRepository.findByIdAndUser(studyGoalId, user)
                .orElseThrow(() -> new RuntimeException("학습목표를 찾을 수 없습니다."));
        
        List<Timer> goalTimers = timerRepository.findByUserAndStudyGoal(user, studyGoal);
        return goalTimers.stream()
                .mapToInt(Timer::getStudyTime)
                .sum() / 60; // 초 -> 분 변환
    }
    
    // 학습목표별 세션 수 조회
    public int getSessionCountByGoal(User user, Long studyGoalId) {
        StudyGoal studyGoal = studyGoalRepository.findByIdAndUser(studyGoalId, user)
                .orElseThrow(() -> new RuntimeException("학습목표를 찾을 수 없습니다."));
        
        return timerRepository.countByUserAndStudyGoal(user, studyGoal);
    }
    
    // 월별 학습시간 조회
    public int getMonthStudyTime(User user, int year, int month) {
        LocalDateTime startOfMonth = LocalDate.of(year, month, 1).atStartOfDay();
        LocalDateTime endOfMonth = LocalDate.of(year, month, 1)
                .plusMonths(1)
                .minusDays(1)
                .atTime(23, 59, 59);
        
        List<Timer> monthTimers = timerRepository.findByUserAndStartTimeBetween(user, startOfMonth, endOfMonth);
        return monthTimers.stream()
                .mapToInt(Timer::getStudyTime)
                .sum() / 60; // 초 -> 분 변환
    }
    
    // 연도별 학습시간 조회
    public int getYearStudyTime(User user, int year) {
        LocalDateTime startOfYear = LocalDate.of(year, 1, 1).atStartOfDay();
        LocalDateTime endOfYear = LocalDate.of(year, 12, 31).atTime(23, 59, 59);
        
        List<Timer> yearTimers = timerRepository.findByUserAndStartTimeBetween(user, startOfYear, endOfYear);
        return yearTimers.stream()
                .mapToInt(Timer::getStudyTime)
                .sum() / 60; // 초 -> 분 변환
    }
}