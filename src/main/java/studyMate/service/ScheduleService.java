package studyMate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studyMate.dto.schedule.ScheduleRequest;
import studyMate.dto.schedule.ScheduleResponse;
import studyMate.entity.Schedule;
import studyMate.entity.StudyTopic;
import studyMate.entity.User;
import studyMate.exception.AccessDeniedException;
import studyMate.exception.ScheduleNotFoundException;
import studyMate.exception.ValidationException;
import studyMate.repository.ScheduleRepository;
import studyMate.repository.StudyTopicRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final StudyTopicRepository studyTopicRepository;

    // === 공통 메서드 ===

    /**
     * 스케줄 조회 및 권한 확인
     * @param user 요청 사용자
     * @param scheduleId 스케줄 ID
     * @return 권한이 있는 스케줄 엔티티
     * @throws ScheduleNotFoundException 스케줄을 찾을 수 없을 때
     * @throws AccessDeniedException 권한이 없을 때
     */
    private Schedule findScheduleWithPermission(User user, String scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ScheduleNotFoundException(scheduleId));

        if (!schedule.getUser().getId().equals(user.getId())) {
            log.warn("권한 없는 스케줄 접근 시도 - 사용자: {}, 스케줄 소유자: {}, 스케줄 ID: {}", 
                    user.getId(), schedule.getUser().getId(), scheduleId);
            throw new AccessDeniedException("스케줄", scheduleId, user.getId());
        }

        return schedule;
    }

    // === CRUD 작업 ===

    // 스케줄 생성
    @Transactional
    public ScheduleResponse createSchedule(User user, ScheduleRequest request) {
        log.info("스케줄 생성 요청 - 사용자: {}, 제목: {}", user.getNickname(), request.getTitle());

        // StudyTopic 조회 (선택적)
        StudyTopic topic = null;
        if (request.getTopicId() != null) {
            topic = studyTopicRepository.findById(request.getTopicId())
                    .orElse(null);
        }

        Schedule schedule = Schedule.builder()
                .user(user)
                .topic(topic)
                .title(request.getTitle())
                .subtitle(request.getSubtitle())
                .description(request.getDescription())
                .color(request.getColor())
                .scheduleDate(request.getScheduleDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .isAllDay(request.getIsAllDay() != null ? request.getIsAllDay() : false)
                .isRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : false)
                .recurrenceRule(request.getRecurrenceRule())
                .studyMode(request.getStudyMode())
                .plannedStudyMinutes(request.getPlannedStudyMinutes())
                .plannedBreakMinutes(request.getPlannedBreakMinutes())
                .studyGoal(request.getStudyGoal())
                .difficulty(request.getDifficulty())
                .reminderMinutes(request.getReminderMinutes())
                .isReminderEnabled(request.getIsReminderEnabled() != null ? request.getIsReminderEnabled() : true)
                .build();

        Schedule savedSchedule = scheduleRepository.save(schedule);
        log.info("스케줄 생성 완료 - ID: {}", savedSchedule.getId());

        return ScheduleResponse.from(savedSchedule);
    }

    // 스케줄 조회 (단일)
    @Transactional(readOnly = true)
    public ScheduleResponse getSchedule(User user, String scheduleId) {
        log.info("스케줄 조회 요청 - 사용자: {}, 스케줄 ID: {}", user.getNickname(), scheduleId);

        Schedule schedule = findScheduleWithPermission(user, scheduleId);
        return ScheduleResponse.from(schedule);
    }

    // 스케줄 수정
    @Transactional
    public ScheduleResponse updateSchedule(User user, String scheduleId, ScheduleRequest request) {
        log.info("스케줄 수정 요청 - 사용자: {}, 스케줄 ID: {}", user.getNickname(), scheduleId);

        Schedule schedule = findScheduleWithPermission(user, scheduleId);

        // StudyTopic 조회 (선택적)
        StudyTopic topic = null;
        if (request.getTopicId() != null) {
            topic = studyTopicRepository.findById(request.getTopicId())
                    .orElse(null);
        }

        // 스케줄 정보 업데이트 (엔티티 메서드 사용)
        schedule.updateFromRequest(request, topic);

        Schedule updatedSchedule = scheduleRepository.save(schedule);
        log.info("스케줄 수정 완료 - ID: {}", updatedSchedule.getId());

        return ScheduleResponse.from(updatedSchedule);
    }

    // 스케줄 삭제
    @Transactional
    public void deleteSchedule(User user, String scheduleId) {
        log.info("스케줄 삭제 요청 - 사용자: {}, 스케줄 ID: {}", user.getNickname(), scheduleId);

        Schedule schedule = findScheduleWithPermission(user, scheduleId);

        scheduleRepository.delete(schedule);
        log.info("스케줄 삭제 완료 - ID: {}", scheduleId);
    }

    // === 조회 작업 ===

    // 사용자의 모든 스케줄 조회
    @Transactional(readOnly = true)
    public List<ScheduleResponse> getAllSchedules(User user) {
        log.info("전체 스케줄 조회 요청 - 사용자: {}", user.getNickname());

        List<Schedule> schedules = scheduleRepository.findByUserOrderByScheduleDateDescStartTimeAsc(user);
        return schedules.stream()
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());
    }

    // 특정 날짜 범위 스케줄 조회
    @Transactional(readOnly = true)
    public List<ScheduleResponse> getSchedulesByDateRange(User user, LocalDate startDate, LocalDate endDate) {
        log.info("날짜 범위 스케줄 조회 요청 - 사용자: {}, 기간: {} ~ {}", user.getNickname(), startDate, endDate);

        List<Schedule> schedules = scheduleRepository.findByUserAndDateRange(user, startDate, endDate);
        return schedules.stream()
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());
    }

    // 특정 날짜 스케줄 조회
    @Transactional(readOnly = true)
    public List<ScheduleResponse> getSchedulesByDate(User user, LocalDate date) {
        log.info("특정 날짜 스케줄 조회 요청 - 사용자: {}, 날짜: {}", user.getNickname(), date);

        List<Schedule> schedules = scheduleRepository.findByUserAndDate(user, date);
        return schedules.stream()
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());
    }

    // 오늘의 스케줄 조회
    @Transactional(readOnly = true)
    public List<ScheduleResponse> getTodaySchedules(User user) {
        log.info("오늘의 스케줄 조회 요청 - 사용자: {}", user.getNickname());

        List<Schedule> schedules = scheduleRepository.findTodaySchedules(user);
        return schedules.stream()
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());
    }

    // 완료된 스케줄 조회
    @Transactional(readOnly = true)
    public List<ScheduleResponse> getCompletedSchedules(User user) {
        log.info("완료된 스케줄 조회 요청 - 사용자: {}", user.getNickname());

        List<Schedule> schedules = scheduleRepository.findCompletedSchedulesByUser(user);
        return schedules.stream()
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());
    }

    // 진행 중인 스케줄 조회
    @Transactional(readOnly = true)
    public List<ScheduleResponse> getInProgressSchedules(User user) {
        log.info("진행 중인 스케줄 조회 요청 - 사용자: {}", user.getNickname());

        List<Schedule> schedules = scheduleRepository.findInProgressSchedulesByUser(user);
        return schedules.stream()
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());
    }

    // === 상태 관리 ===

    // 스케줄 상태 변경
    @Transactional
    public ScheduleResponse updateScheduleStatus(User user, String scheduleId, Schedule.ScheduleStatus status) {
        log.info("스케줄 상태 변경 요청 - 사용자: {}, 스케줄 ID: {}, 상태: {}", user.getNickname(), scheduleId, status);

        Schedule schedule = findScheduleWithPermission(user, scheduleId);

        schedule.setStatus(status);
        
        // 완료 상태로 변경 시 완료율을 100%로 설정
        if (status == Schedule.ScheduleStatus.COMPLETED) {
            schedule.setCompletionRate(100);
        }

        Schedule updatedSchedule = scheduleRepository.save(schedule);
        log.info("스케줄 상태 변경 완료 - ID: {}, 상태: {}", updatedSchedule.getId(), status);

        return ScheduleResponse.from(updatedSchedule);
    }

    // 완료율 업데이트
    @Transactional
    public ScheduleResponse updateCompletionRate(User user, String scheduleId, Integer completionRate) {
        log.info("완료율 업데이트 요청 - 사용자: {}, 스케줄 ID: {}, 완료율: {}%", user.getNickname(), scheduleId, completionRate);

        Schedule schedule = findScheduleWithPermission(user, scheduleId);

        // 완료율 범위 검증 (0-100)
        if (completionRate < 0 || completionRate > 100) {
            throw new ValidationException("완료율", "0에서 100 사이의 값이어야 합니다");
        }

        schedule.setCompletionRate(completionRate);
        
        // 완료율이 100%이면 상태를 완료로 변경
        if (completionRate == 100) {
            schedule.setStatus(Schedule.ScheduleStatus.COMPLETED);
        }

        Schedule updatedSchedule = scheduleRepository.save(schedule);
        log.info("완료율 업데이트 완료 - ID: {}, 완료율: {}%", updatedSchedule.getId(), completionRate);

        return ScheduleResponse.from(updatedSchedule);
    }
} 