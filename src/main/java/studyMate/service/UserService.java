package studyMate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studyMate.dto.auth.SignUpReqDto;
import studyMate.entity.User;
import studyMate.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

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






}