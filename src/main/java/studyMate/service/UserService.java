package studyMate.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studyMate.dto.SignInReq;
import studyMate.dto.TokenDto;
import studyMate.dto.auth.SignUpReqDto;
import studyMate.entity.RefreshToken;
import studyMate.entity.User;
import studyMate.repository.RefreshTokenRepository;
import studyMate.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public void registerUser(SignUpReqDto signUpReqDto) {
        // 이메일 인증 여부 확인
        if (!authService.isEmailVerified(signUpReqDto.getEmail())) {
            throw new IllegalArgumentException("이메일 인증이 완료되지 않았습니다. 먼저 이메일 인증을 완료해주세요.");
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(signUpReqDto.getPassword());

        // User 엔티티 생성
        User user = User.builder()
                .email(signUpReqDto.getEmail())
                .password(encodedPassword)
                .nickname(signUpReqDto.getNickname())
                .age(signUpReqDto.getAge())
                .sex(signUpReqDto.getSex())
                .build();

        // 사용자 저장
        userRepository.save(user);
    }

    @Transactional
    public TokenDto login(@Valid SignInReq signInReq) {
        // 사용자 검증
        User user = userRepository.findByEmail(signInReq.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        if (!passwordEncoder.matches(signInReq.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // Access Token 생성
        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail());
        
        // Refresh Token 생성
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

        // RefreshToken 저장
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .key(user.getEmail())
                .value(refreshToken)
                .build();
        
        refreshTokenRepository.save(refreshTokenEntity);

        return TokenDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .accessTokenExpiresIn(jwtTokenProvider.getAccessTokenExpirationTime())
                .build();
    }
}