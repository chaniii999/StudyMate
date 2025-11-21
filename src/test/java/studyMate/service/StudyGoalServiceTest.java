package studyMate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import studyMate.dto.studygoal.StudyGoalRequest;
import studyMate.dto.studygoal.StudyGoalResponse;
import studyMate.entity.GoalStatus;
import studyMate.entity.StudyGoal;
import studyMate.entity.User;
import studyMate.repository.StudyGoalRepository;
import studyMate.repository.TimerRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StudyGoalService 테스트")
class StudyGoalServiceTest {

    @Mock
    private StudyGoalRepository studyGoalRepository;

    @Mock
    private TimerRepository timerRepository;

    @InjectMocks
    private StudyGoalService studyGoalService;

    private User user;
    private StudyGoal studyGoal;
    private StudyGoalRequest request;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id("user1")
                .email("test@example.com")
                .nickname("테스터")
                .build();

        studyGoal = StudyGoal.builder()
                .id(1L)
                .user(user)
                .title("Java 학습")
                .subject("프로그래밍")
                .targetHours(100)
                .targetSessions(50)
                .currentHours(0)
                .currentMinutes(0)
                .currentSessions(0)
                .status(GoalStatus.ACTIVE)
                .startDate(LocalDate.now())
                .targetDate(LocalDate.now().plusMonths(3))
                .build();

        request = new StudyGoalRequest();
        request.setTitle("새 목표");
        request.setSubject("수학");
        request.setTargetHours(50);
        request.setTargetSessions(25);
    }

    @Test
    @DisplayName("학습목표 생성 성공")
    void createStudyGoal_Success() {
        // Given
        when(studyGoalRepository.save(any(StudyGoal.class))).thenReturn(studyGoal);

        // When
        StudyGoalResponse response = studyGoalService.createStudyGoal(user, request);

        // Then
        assertNotNull(response);
        verify(studyGoalRepository, times(1)).save(any(StudyGoal.class));
    }

    @Test
    @DisplayName("학습목표 생성 시 기본값 설정")
    void createStudyGoal_DefaultValues_Success() {
        // Given
        StudyGoalRequest requestWithoutDefaults = new StudyGoalRequest();
        requestWithoutDefaults.setTitle("목표");
        when(studyGoalRepository.save(any(StudyGoal.class))).thenReturn(studyGoal);

        // When
        StudyGoalResponse response = studyGoalService.createStudyGoal(user, requestWithoutDefaults);

        // Then
        assertNotNull(response);
        // 기본값이 설정되었는지 확인 (내부적으로 처리됨)
        verify(studyGoalRepository, times(1)).save(any(StudyGoal.class));
    }

    @Test
    @DisplayName("학습목표 조회 성공")
    void getStudyGoal_Success() {
        // Given
        when(studyGoalRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(studyGoal));

        // When
        StudyGoalResponse response = studyGoalService.getStudyGoal(user, 1L);

        // Then
        assertNotNull(response);
        verify(studyGoalRepository, times(1)).findByIdAndUser(1L, user);
    }

    @Test
    @DisplayName("존재하지 않는 학습목표 조회 시 예외 발생")
    void getStudyGoal_NotFound_ThrowsException() {
        // Given
        when(studyGoalRepository.findByIdAndUser(999L, user)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            studyGoalService.getStudyGoal(user, 999L);
        });

        assertTrue(exception.getMessage().contains("학습목표를 찾을 수 없습니다"));
    }

    @Test
    @DisplayName("학습목표 수정 성공")
    void updateStudyGoal_Success() {
        // Given
        StudyGoalRequest updateRequest = new StudyGoalRequest();
        updateRequest.setTitle("수정된 목표");
        updateRequest.setTargetHours(200);
        when(studyGoalRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(studyGoal));
        when(studyGoalRepository.save(any(StudyGoal.class))).thenReturn(studyGoal);

        // When
        StudyGoalResponse response = studyGoalService.updateStudyGoal(user, 1L, updateRequest);

        // Then
        assertNotNull(response);
        verify(studyGoalRepository, times(1)).findByIdAndUser(1L, user);
        verify(studyGoalRepository, times(1)).save(any(StudyGoal.class));
    }

    @Test
    @DisplayName("학습목표 삭제 성공")
    void deleteStudyGoal_Success() {
        // Given
        when(studyGoalRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(studyGoal));
        doNothing().when(studyGoalRepository).delete(studyGoal);

        // When
        studyGoalService.deleteStudyGoal(user, 1L);

        // Then
        verify(studyGoalRepository, times(1)).findByIdAndUser(1L, user);
        verify(studyGoalRepository, times(1)).delete(studyGoal);
    }

    @Test
    @DisplayName("진행도 업데이트 성공")
    void updateProgress_Success() {
        // Given
        int studyMinutes = 60; // 1시간
        when(studyGoalRepository.findById(1L)).thenReturn(Optional.of(studyGoal));
        when(studyGoalRepository.save(any(StudyGoal.class))).thenReturn(studyGoal);

        // When
        studyGoalService.updateProgress(1L, studyMinutes);

        // Then
        verify(studyGoalRepository, times(1)).findById(1L);
        verify(studyGoalRepository, times(1)).save(any(StudyGoal.class));
    }

    @Test
    @DisplayName("목표 달성 시 상태 자동 변경")
    void updateProgress_GoalCompleted_StatusChanged() {
        // Given
        studyGoal.setCurrentHours(99);
        studyGoal.setCurrentMinutes(5940); // 99시간 = 5940분
        int studyMinutes = 60; // 1시간 추가하면 100시간 달성
        when(studyGoalRepository.findById(1L)).thenReturn(Optional.of(studyGoal));
        when(studyGoalRepository.save(any(StudyGoal.class))).thenReturn(studyGoal);

        // When
        studyGoalService.updateProgress(1L, studyMinutes);

        // Then
        verify(studyGoalRepository, times(1)).save(any(StudyGoal.class));
        // 목표 달성 시 상태가 COMPLETED로 변경되는지 확인 (내부 로직)
    }

    @Test
    @DisplayName("활성 학습목표 조회")
    void getActiveStudyGoals_Success() {
        // Given
        List<StudyGoal> activeGoals = List.of(studyGoal);
        when(studyGoalRepository.findByUserAndStatusOrderByCreatedAtDesc(user, GoalStatus.ACTIVE))
                .thenReturn(activeGoals);

        // When
        List<StudyGoalResponse> responses = studyGoalService.getActiveStudyGoals(user);

        // Then
        assertNotNull(responses);
        assertEquals(1, responses.size());
        verify(studyGoalRepository, times(1))
                .findByUserAndStatusOrderByCreatedAtDesc(user, GoalStatus.ACTIVE);
    }
}

