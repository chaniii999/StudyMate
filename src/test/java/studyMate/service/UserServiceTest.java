package studyMate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import studyMate.dto.auth.LoginResponseDto;
import studyMate.dto.auth.SignInReq;
import studyMate.dto.auth.SignUpReqDto;
import studyMate.entity.RefreshToken;
import studyMate.entity.User;
import studyMate.repository.RefreshTokenRepository;
import studyMate.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthService authService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private UserService userService;

    private User user;
    private SignUpReqDto signUpReqDto;
    private SignInReq signInReq;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id("user1")
                .email("test@example.com")
                .password("encodedPassword")
                .nickname("테스터")
                .age(25)
                .sex("M")
                .build();

        signUpReqDto = SignUpReqDto.builder()
                .email("test@example.com")
                .password("password123")
                .nickname("테스터")
                .age(25)
                .sex("M")
                .build();

        signInReq = new SignInReq();
        signInReq.setEmail("test@example.com");
        signInReq.setPassword("password123");
    }

    @Test
    @DisplayName("회원가입 성공")
    void registerUser_Success() {
        // Given
        when(authService.isEmailVerified(signUpReqDto.getEmail())).thenReturn(true);
        when(passwordEncoder.encode(signUpReqDto.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        userService.registerUser(signUpReqDto);

        // Then
        verify(authService, times(1)).isEmailVerified(signUpReqDto.getEmail());
        verify(passwordEncoder, times(1)).encode(signUpReqDto.getPassword());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("이메일 인증 미완료 시 회원가입 실패")
    void registerUser_EmailNotVerified_ThrowsException() {
        // Given
        when(authService.isEmailVerified(signUpReqDto.getEmail())).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(signUpReqDto);
        });

        assertTrue(exception.getMessage().contains("이메일 인증이 완료되지 않았습니다"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("로그인 성공")
    void login_Success() {
        // Given
        String accessToken = "access.token";
        String refreshToken = "refresh.token";
        when(userRepository.findByEmail(signInReq.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(signInReq.getPassword(), user.getPassword())).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(user.getEmail())).thenReturn(accessToken);
        when(jwtTokenProvider.createRefreshToken(user.getEmail())).thenReturn(refreshToken);
        when(jwtTokenProvider.getAccessTokenExpirationTime()).thenReturn(3600L);
        when(refreshTokenRepository.findByKey(user.getEmail())).thenReturn(Optional.empty());
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(new RefreshToken());

        // When
        LoginResponseDto response = userService.login(signInReq);

        // Then
        assertNotNull(response);
        assertEquals(user.getId(), response.getUserId());
        assertEquals(user.getEmail(), response.getEmail());
        assertEquals(user.getNickname(), response.getNickname());
        assertNotNull(response.getToken());
        assertEquals(accessToken, response.getToken().getAccessToken());
        assertEquals(refreshToken, response.getToken().getRefreshToken());
        verify(userRepository, times(1)).findByEmail(signInReq.getEmail());
        verify(passwordEncoder, times(1)).matches(signInReq.getPassword(), user.getPassword());
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인 시 예외 발생")
    void login_UserNotFound_ThrowsException() {
        // Given
        when(userRepository.findByEmail(signInReq.getEmail())).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.login(signInReq);
        });

        assertTrue(exception.getMessage().contains("이메일 또는 비밀번호가 올바르지 않습니다"));
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인 시 예외 발생")
    void login_WrongPassword_ThrowsException() {
        // Given
        when(userRepository.findByEmail(signInReq.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(signInReq.getPassword(), user.getPassword())).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.login(signInReq);
        });

        assertTrue(exception.getMessage().contains("이메일 또는 비밀번호가 올바르지 않습니다"));
        verify(jwtTokenProvider, never()).createAccessToken(anyString());
    }

    @Test
    @DisplayName("로그인 시 기존 리프레시 토큰 삭제")
    void login_ExistingRefreshToken_DeletesOldToken() {
        // Given
        String accessToken = "access.token";
        String refreshToken = "refresh.token";
        RefreshToken existingToken = RefreshToken.builder()
                .key(user.getEmail())
                .value("old.refresh.token")
                .build();

        when(userRepository.findByEmail(signInReq.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(signInReq.getPassword(), user.getPassword())).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(user.getEmail())).thenReturn(accessToken);
        when(jwtTokenProvider.createRefreshToken(user.getEmail())).thenReturn(refreshToken);
        when(jwtTokenProvider.getAccessTokenExpirationTime()).thenReturn(3600L);
        when(refreshTokenRepository.findByKey(user.getEmail())).thenReturn(Optional.of(existingToken));
        doNothing().when(refreshTokenRepository).delete(existingToken);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(new RefreshToken());

        // When
        userService.login(signInReq);

        // Then
        verify(refreshTokenRepository, times(1)).delete(existingToken);
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }
}

