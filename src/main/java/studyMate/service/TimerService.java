package studyMate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studyMate.dto.pomodoro.TimerReqDto;
import studyMate.dto.pomodoro.TimerResDto;
import studyMate.entity.StudyGoal;
import studyMate.entity.Timer;
import studyMate.entity.User;
import studyMate.exception.StudyGoalNotFoundException;
import studyMate.repository.StudyGoalRepository;
import studyMate.repository.TimerRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimerService {
    
    private final TimerRepository timerRepository;
    private final StudyGoalRepository studyGoalRepository;
    private final StudyGoalService studyGoalService;
    
    // 사용자별 타이머 상태를 저장하는 맵
    private final Map<String, TimerStatus> userTimerStatus = new ConcurrentHashMap<>();
    
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
                .orElseThrow(() -> new StudyGoalNotFoundException(studyGoalId));
        return timerRepository.findByUserAndStudyGoal(user, studyGoal);
    }
    
    // 타이머 기록 저장 (학습목표 연동)
    @Transactional
    public Timer saveTimerRecord(User user, Timer timer, Long studyGoalId) {
        timer.setUser(user);
        
        // 학습목표가 지정된 경우 연결
        if (studyGoalId != null) {
            StudyGoal studyGoal = studyGoalRepository.findByIdAndUser(studyGoalId, user)
                    .orElseThrow(() -> new StudyGoalNotFoundException(studyGoalId));
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
        Timer timer = timerRepository.findById(timerId)
                .orElse(null);
        
        if (timer == null) {
            log.warn("타이머 기록을 찾을 수 없습니다: {} (요청 사용자: {})", timerId, user.getEmail());
            return false;
        }
        
        // User ID로 비교 (equals 대신 ID 비교로 변경)
        if (!timer.getUser().getId().equals(user.getId())) {
            log.warn("타이머 삭제 권한 없음: {} (타이머 소유자: {}, 요청 사용자: {})", 
                    timerId, timer.getUser().getId(), user.getId());
            return false;
        }
        
        // 학습목표가 연결된 경우 진행도 차감
        if (timer.getStudyGoal() != null) {
            StudyGoal studyGoal = timer.getStudyGoal();
            int studyMinutes = timer.getStudyTime() / 60;
            double hoursToDeduct = studyMinutes / 60.0; // 소수점 포함 계산
            
            if (hoursToDeduct > 0) {
                double newHours = Math.max(0.0, studyGoal.getCurrentHours() - hoursToDeduct);
                studyGoal.setCurrentHours((int) Math.round(newHours)); // 반올림 처리
            }
            studyGoal.setCurrentSessions(Math.max(0, studyGoal.getCurrentSessions() - 1));
            
            studyGoalRepository.save(studyGoal);
            log.info("타이머 기록 삭제로 학습목표 진행도 차감: {} ({}시간, 1세션)", 
                    studyGoal.getTitle(), hoursToDeduct);
        }
        
        timerRepository.delete(timer);
        log.info("타이머 기록 삭제 성공: {} (사용자: {})", timerId, user.getEmail());
        return true;
    }
    
    // 사용자의 오늘 학습시간 조회
    public int getTodayStudyTime(User user) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);
        
        List<Timer> todayTimers = timerRepository.findByUserAndStartTimeBetween(user, startOfDay, endOfDay);
        
        return todayTimers.stream()
                .mapToInt(Timer::getStudyTime)
                .sum() / 60; // 초 -> 분 변환
    }
    
    // 사용자의 이번주 학습시간 조회
    public int getWeekStudyTime(User user) {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDateTime startOfWeekDateTime = startOfWeek.atStartOfDay();
        LocalDateTime endOfToday = today.atTime(23, 59, 59);
        
        List<Timer> weekTimers = timerRepository.findByUserAndStartTimeBetween(user, startOfWeekDateTime, endOfToday);
        
        return weekTimers.stream()
                .mapToInt(Timer::getStudyTime)
                .sum() / 60; // 초 -> 분 변환
    }
    
    // 사용자의 총 학습시간 조회 (DB 집계 쿼리 사용으로 성능 개선)
    public int getTotalStudyTime(User user) {
        int totalSeconds = timerRepository.sumStudyTimeByUser(user);
        return totalSeconds / 60; // 초 -> 분 변환
    }
    
    // 사용자의 총 학습 세션 수 조회
    public int getTotalSessionCount(User user) {
        return timerRepository.countByUser(user);
    }
    
    // 사용자의 평균 세션 시간 조회 (분) - DB 집계 쿼리 사용으로 성능 개선
    public double getAverageSessionTime(User user) {
        double avgSeconds = timerRepository.avgStudyTimeByUser(user);
        return avgSeconds / 60.0; // 초 -> 분 변환 (소수점 포함)
    }
    
    // 최장 학습 세션 시간 조회 (분) - DB 집계 쿼리 사용으로 성능 개선
    public int getLongestSessionTime(User user) {
        int maxSeconds = timerRepository.maxStudyTimeByUser(user);
        return maxSeconds / 60; // 초 -> 분 변환
    }
    
    // 학습목표별 총 학습시간 조회
    public int getStudyTimeByGoal(User user, Long studyGoalId) {
        StudyGoal studyGoal = studyGoalRepository.findByIdAndUser(studyGoalId, user)
                .orElseThrow(() -> new StudyGoalNotFoundException(studyGoalId));
        
        List<Timer> goalTimers = timerRepository.findByUserAndStudyGoal(user, studyGoal);
        return goalTimers.stream()
                .mapToInt(Timer::getStudyTime)
                .sum() / 60; // 초 -> 분 변환
    }
    
    // 학습목표별 세션 수 조회
    public int getSessionCountByGoal(User user, Long studyGoalId) {
        StudyGoal studyGoal = studyGoalRepository.findByIdAndUser(studyGoalId, user)
                .orElseThrow(() -> new StudyGoalNotFoundException(studyGoalId));
        
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

    @Transactional
    public TimerResDto startTimer(User user, TimerReqDto dto) {
        String timerType = dto.getTimerType() != null ? dto.getTimerType() : "STUDY";

        // 현재 사용자의 타이머 상태 저장 또는 업데이트
        TimerStatus status = userTimerStatus.getOrDefault(user.getId(),
                new TimerStatus(dto.getStudyTimes(), dto.getBreakTimes()));

        status.setStatus("STARTED");
        status.setTimerType(timerType);
        status.setCurrentDuration(status.calculateNewDuration());
        status.startSession(); // 세션 시작

        userTimerStatus.put(user.getId(), status);

        return buildTimerResponse(true,
                status.isStudyMode() ? "학습 타이머가 시작되었습니다." : "휴식 타이머가 시작되었습니다.",
                status, user);
    }

    @Transactional
    public TimerResDto stopTimer(User user) {
        TimerStatus status = userTimerStatus.remove(user.getId());
        if (status == null) {
            return buildErrorResponse("실행 중인 타이머가 없습니다.");
        }

        // 세션 종료 시 실제 경과 시간 업데이트
        status.endSession();

        log.info("타이머 중지 - 사용자: {}, 실제 학습시간: {}분, 실제 휴식시간: {}분",
                user.getNickname(), status.getActualStudyMinutes(), status.getActualRestMinutes());

        return buildTimerResponse(true, "타이머가 중지되었습니다.", status, user);
    }

    @Transactional
    public TimerResDto pauseTimer(User user) {
        TimerStatus status = userTimerStatus.get(user.getId());
        if (status == null) {
            return buildErrorResponse("실행 중인 타이머가 없습니다.");
        }

        status.setStatus("PAUSED");
        status.setRemainingTime(status.calculateRemainingTime());
        userTimerStatus.put(user.getId(), status);

        return buildTimerResponse(true, "타이머가 일시정지되었습니다.", status, user);
    }

    @Transactional
    public TimerResDto switchTimer(User user) {
        TimerStatus status = userTimerStatus.get(user.getId());
        if (status == null) {
            return buildErrorResponse("실행 중인 타이머가 없습니다.");
        }

        status.updateForNewCycle();
        userTimerStatus.put(user.getId(), status);

        return buildTimerResponse(true,
                status.isStudyMode() ? "학습 타이머가 시작되었습니다." : "휴식 타이머가 시작되었습니다.",
                status, user);
    }

    @Transactional
    public Timer saveTimerRecord(User user, int studyMinutes, int restMinutes, LocalDateTime startTime, LocalDateTime endTime, String mode, String summary) {
        // 실제 경과 시간 계산 (초 단위)
        int actualStudySeconds = 0;
        int actualRestSeconds = 0;

        if (startTime != null && endTime != null) {
            long totalSeconds = java.time.Duration.between(startTime, endTime).getSeconds();

            // 모드에 따라 학습/휴식 시간 분배
            if ("STUDY".equals(mode) || mode == null) {
                actualStudySeconds = (int) totalSeconds;
                actualRestSeconds = 0;
            } else if ("BREAK".equals(mode)) {
                actualStudySeconds = 0;
                actualRestSeconds = (int) totalSeconds;
            } else {
                // 포모도로 모드 등에서는 설정된 비율로 분배
                actualStudySeconds = studyMinutes * 60;
                actualRestSeconds = restMinutes * 60;
            }
        } else {
            // startTime/endTime이 없는 경우 분 단위를 초로 변환
            actualStudySeconds = studyMinutes * 60;
            actualRestSeconds = restMinutes * 60;
        }

        Timer timer = Timer.builder()
                .user(user)
                .studyTime(actualStudySeconds) // 초 단위로 저장
                .restTime(actualRestSeconds)   // 초 단위로 저장
                .startTime(startTime)
                .endTime(endTime)
                .mode(mode)
                .summary(summary)
                .build();

        log.info("타이머 기록 저장 - 사용자: {}, 실제 학습시간: {}초({}분), 실제 휴식시간: {}초({}분), 총 경과시간: {}분",
                user.getNickname(), actualStudySeconds, actualStudySeconds/60, actualRestSeconds, actualRestSeconds/60,
                startTime != null && endTime != null ? java.time.Duration.between(startTime, endTime).toMinutes() : 0);

        return timerRepository.save(timer);
    }

    private TimerResDto buildErrorResponse(String message) {
        return TimerResDto.builder()
                .success(false)
                .message(message)
                .status("STOPPED")
                .remainingTime(0)
                .build();
    }

    private TimerResDto buildTimerResponse(boolean success, String message, TimerStatus status, User user) {
        return TimerResDto.builder()
                .success(success)
                .message(message)
                .status(status.getStatus())
                .remainingTime(status.calculateRemainingTime())
                .timerType(status.getTimerType())
                .userNickname(user.getNickname())
                .studyTimes(status.getStudyMinutes())
                .breakTimes(status.getBreakMinutes())
                .cycleCount(status.getCycleCount())
                .build();
    }

}