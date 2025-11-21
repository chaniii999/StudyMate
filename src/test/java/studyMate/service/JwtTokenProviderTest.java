package studyMate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import studyMate.config.JwtProperties;
import studyMate.entity.User;
import studyMate.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenProvider 테스트")
class JwtTokenProviderTest {

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    private User user;
    private String email;
    private String secretKey;

    @BeforeEach
    void setUp() {
        email = "test@example.com";
        secretKey = "testSecretKeyForJwtTokenProviderTestMustBeAtLeast256BitsLong";
        
        user = User.builder()
                .id("user1")
                .email(email)
                .nickname("테스터")
                .build();
    }
    
    private void setupSecretKey() {
        when(jwtProperties.getSecretKey()).thenReturn(secretKey);
    }
    
    private void setupAccessTokenProperties() {
        setupSecretKey();
        when(jwtProperties.getAccessTokenValidityInSeconds()).thenReturn(3600L); // 1시간
    }
    
    private void setupRefreshTokenProperties() {
        setupSecretKey();
        when(jwtProperties.getRefreshTokenValidityInSeconds()).thenReturn(86400L); // 24시간
    }

    @Test
    @DisplayName("액세스 토큰 생성 성공")
    void createAccessToken_Success() {
        // Given
        setupAccessTokenProperties();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // When
        String token = jwtTokenProvider.createAccessToken(email);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        verify(userRepository, times(1)).findByEmail(email);
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 토큰 생성 시 예외 발생")
    void createAccessToken_UserNotFound_ThrowsException() {
        // Given
        String nonExistentEmail = "nonexistent@example.com";
        when(userRepository.findByEmail(nonExistentEmail)).thenReturn(Optional.empty());
        // secretKey는 사용자 조회 전에 필요 없음

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            jwtTokenProvider.createAccessToken(nonExistentEmail);
        });

        assertTrue(exception.getMessage().contains("User not found"));
    }

    @Test
    @DisplayName("리프레시 토큰 생성 성공")
    void createRefreshToken_Success() {
        // Given
        setupRefreshTokenProperties();
        
        // When
        String token = jwtTokenProvider.createRefreshToken(email);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        // 리프레시 토큰은 사용자 조회 없이 생성
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("유효한 토큰 검증 성공")
    void validateToken_ValidToken_ReturnsTrue() {
        // Given
        setupAccessTokenProperties();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        String token = jwtTokenProvider.createAccessToken(email);

        // When
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("유효하지 않은 토큰 검증 실패")
    void validateToken_InvalidToken_ReturnsFalse() {
        // Given
        setupSecretKey(); // 토큰 파싱 시 getSigningKey() 호출될 수 있음
        String invalidToken = "invalid.token.string";

        // When
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("토큰에서 사용자 이메일 추출 성공")
    void getUsername_ValidToken_ReturnsEmail() {
        // Given
        setupAccessTokenProperties();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        String token = jwtTokenProvider.createAccessToken(email);

        // When
        String extractedEmail = jwtTokenProvider.getUsername(token);

        // Then
        assertEquals(email, extractedEmail);
    }

    @Test
    @DisplayName("토큰에서 Claims 추출 성공")
    void getClaimsFromToken_ValidToken_ReturnsClaims() {
        // Given
        setupAccessTokenProperties();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        String token = jwtTokenProvider.createAccessToken(email);

        // When
        var claims = jwtTokenProvider.getClaimsFromToken(token);

        // Then
        assertNotNull(claims);
        assertEquals(email, claims.get("email"));
        assertEquals("user1", claims.get("id"));
        assertEquals("테스터", claims.get("nickname"));
    }

    @Test
    @DisplayName("토큰 만료 시간 확인")
    void getTokenExpirationTime_ValidToken_ReturnsPositiveValue() {
        // Given
        setupAccessTokenProperties();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        String token = jwtTokenProvider.createAccessToken(email);

        // When
        long expirationTime = jwtTokenProvider.getTokenExpirationTime(token);

        // Then
        assertTrue(expirationTime > 0);
        assertTrue(expirationTime <= 3600); // 1시간 이하
    }

    @Test
    @DisplayName("만료되지 않은 토큰 확인")
    void isTokenExpired_NotExpired_ReturnsFalse() {
        // Given
        setupAccessTokenProperties();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        String token = jwtTokenProvider.createAccessToken(email);

        // When
        boolean isExpired = jwtTokenProvider.isTokenExpired(token);

        // Then
        assertFalse(isExpired);
    }
}

