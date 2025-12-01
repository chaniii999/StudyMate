package studyMate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import studyMate.dto.schedule.ScheduleRequest;
import studyMate.dto.schedule.ScheduleResponse;
import studyMate.entity.Schedule;
import studyMate.entity.User;
import studyMate.exception.AccessDeniedException;
import studyMate.exception.ScheduleNotFoundException;
import studyMate.repository.ScheduleRepository;
import studyMate.repository.StudyTopicRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleService 테스트")
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private StudyTopicRepository studyTopicRepository;

    @InjectMocks
    private ScheduleService scheduleService;

    private User user;
    private User otherUser;
    private Schedule schedule;
    private ScheduleRequest request;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id("user1")
                .email("user1@example.com")
                .nickname("사용자1")
                .build();

        otherUser = User.builder()
                .id("user2")
                .email("user2@example.com")
                .nickname("사용자2")
                .build();

        schedule = Schedule.builder()
                .id("schedule1")
                .user(user)
                .title("학습 스케줄")
                .scheduleDate(LocalDate.now())
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .build();

        request = new ScheduleRequest();
        request.setTitle("새 스케줄");
        request.setScheduleDate(LocalDate.now());
        request.setStartTime(LocalTime.of(9, 0));
        request.setEndTime(LocalTime.of(10, 0));
    }

    @Test
    @DisplayName("스케줄 생성 성공")
    void createSchedule_Success() {
        // Given
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(schedule);

        // When
        ScheduleResponse response = scheduleService.createSchedule(user, request);

        // Then
        assertNotNull(response);
        verify(scheduleRepository, times(1)).save(any(Schedule.class));
    }

    @Test
    @DisplayName("스케줄 조회 성공")
    void getSchedule_Success() {
        // Given
        when(scheduleRepository.findById("schedule1")).thenReturn(Optional.of(schedule));

        // When
        ScheduleResponse response = scheduleService.getSchedule(user, "schedule1");

        // Then
        assertNotNull(response);
        verify(scheduleRepository, times(1)).findById("schedule1");
    }

    @Test
    @DisplayName("존재하지 않는 스케줄 조회 시 예외 발생")
    void getSchedule_NotFound_ThrowsException() {
        // Given
        when(scheduleRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        ScheduleNotFoundException exception = assertThrows(ScheduleNotFoundException.class, () -> {
            scheduleService.getSchedule(user, "nonexistent");
        });

        assertTrue(exception.getMessage().contains("스케줄") && exception.getMessage().contains("nonexistent"));
    }

    @Test
    @DisplayName("다른 사용자의 스케줄 조회 시 예외 발생")
    void getSchedule_Unauthorized_ThrowsException() {
        // Given
        when(scheduleRepository.findById("schedule1")).thenReturn(Optional.of(schedule));

        // When & Then
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> {
            scheduleService.getSchedule(otherUser, "schedule1");
        });

        assertTrue(exception.getMessage().contains("권한") || exception.getMessage().contains("스케줄"));
    }

    @Test
    @DisplayName("스케줄 수정 성공")
    void updateSchedule_Success() {
        // Given
        ScheduleRequest updateRequest = new ScheduleRequest();
        updateRequest.setTitle("수정된 스케줄");
        updateRequest.setScheduleDate(LocalDate.now());
        when(scheduleRepository.findById("schedule1")).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any(Schedule.class))).thenReturn(schedule);

        // When
        ScheduleResponse response = scheduleService.updateSchedule(user, "schedule1", updateRequest);

        // Then
        assertNotNull(response);
        verify(scheduleRepository, times(1)).findById("schedule1");
        verify(scheduleRepository, times(1)).save(any(Schedule.class));
    }

    @Test
    @DisplayName("다른 사용자의 스케줄 수정 시 예외 발생")
    void updateSchedule_Unauthorized_ThrowsException() {
        // Given
        ScheduleRequest updateRequest = new ScheduleRequest();
        when(scheduleRepository.findById("schedule1")).thenReturn(Optional.of(schedule));

        // When & Then
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> {
            scheduleService.updateSchedule(otherUser, "schedule1", updateRequest);
        });

        assertTrue(exception.getMessage().contains("권한") || exception.getMessage().contains("스케줄"));
    }

    @Test
    @DisplayName("스케줄 삭제 성공")
    void deleteSchedule_Success() {
        // Given
        when(scheduleRepository.findById("schedule1")).thenReturn(Optional.of(schedule));
        doNothing().when(scheduleRepository).delete(schedule);

        // When
        scheduleService.deleteSchedule(user, "schedule1");

        // Then
        verify(scheduleRepository, times(1)).findById("schedule1");
        verify(scheduleRepository, times(1)).delete(schedule);
    }

    @Test
    @DisplayName("다른 사용자의 스케줄 삭제 시 예외 발생")
    void deleteSchedule_Unauthorized_ThrowsException() {
        // Given
        when(scheduleRepository.findById("schedule1")).thenReturn(Optional.of(schedule));

        // When & Then
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> {
            scheduleService.deleteSchedule(otherUser, "schedule1");
        });

        assertTrue(exception.getMessage().contains("권한") || exception.getMessage().contains("스케줄"));
        verify(scheduleRepository, never()).delete(any());
    }
}

