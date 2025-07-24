package studyMate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import studyMate.dto.ApiResponse;
import studyMate.dto.auth.TokenRefreshRequest;
import studyMate.dto.auth.TokenRefreshResponse;
import studyMate.service.JwtTokenProvider;

@Slf4j
@RestController
@RequestMapping("/api/token")
@RequiredArgsConstructor
public class TokenController {

    private final JwtTokenProvider jwtTokenProvider;

    // 토큰 갱신 API
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshToken(@RequestBody TokenRefreshRequest request) {
        try {
            log.info("토큰 갱신 요청");
            
            // 액세스 토큰에서 이메일 추출
            String email = jwtTokenProvider.getEmailFromToken(request.getAccessToken());
            if (email == null) {
                log.warn("액세스 토큰에서 이메일을 추출할 수 없습니다.");
                return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "유효하지 않은 액세스 토큰입니다.", null));
            }

            // 리프레시 토큰 유효성 검증
            if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
                log.warn("리프레시 토큰이 유효하지 않습니다: {}", email);
                return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "유효하지 않은 리프레시 토큰입니다.", null));
            }

            // 리프레시 토큰에서 이메일 추출 및 일치 확인
            String refreshTokenEmail = jwtTokenProvider.getUsername(request.getRefreshToken());
            if (!email.equals(refreshTokenEmail)) {
                log.warn("액세스 토큰과 리프레시 토큰의 이메일이 일치하지 않습니다: {} vs {}", email, refreshTokenEmail);
                return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "토큰 정보가 일치하지 않습니다.", null));
            }

            // 새로운 액세스 토큰 생성
            String newAccessToken = jwtTokenProvider.refreshAccessToken(email);
            String newRefreshToken = jwtTokenProvider.createRefreshToken(email);

            TokenRefreshResponse response = TokenRefreshResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtTokenProvider.getAccessTokenExpirationTime())
                    .build();

            log.info("토큰 갱신 성공: {}", email);
            return ResponseEntity.ok(new ApiResponse<>(true, "토큰이 성공적으로 갱신되었습니다.", response));

        } catch (Exception e) {
            log.error("토큰 갱신 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "토큰 갱신에 실패했습니다: " + e.getMessage(), null));
        }
    }

    // 토큰 상태 확인 API (앱 접속 시 호출)
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> validateAndRefreshToken(@RequestBody TokenRefreshRequest request) {
        try {
            log.info("토큰 상태 확인 및 갱신 요청");
            
            // 액세스 토큰에서 이메일 추출
            String email = jwtTokenProvider.getEmailFromToken(request.getAccessToken());
            if (email == null) {
                log.warn("액세스 토큰에서 이메일을 추출할 수 없습니다.");
                return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "유효하지 않은 액세스 토큰입니다.", null));
            }

            // 액세스 토큰 유효성 검증
            if (!jwtTokenProvider.validateToken(request.getAccessToken())) {
                log.warn("액세스 토큰이 유효하지 않습니다: {}", email);
                return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "유효하지 않은 액세스 토큰입니다.", null));
            }

            // 리프레시 토큰 유효성 검증
            if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
                log.warn("리프레시 토큰이 유효하지 않습니다: {}", email);
                return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "유효하지 않은 리프레시 토큰입니다.", null));
            }

            // 액세스 토큰 만료까지 남은 시간 확인 (24시간 이내면 갱신)
            long remainingTime = jwtTokenProvider.getTokenExpirationTime(request.getAccessToken());
            boolean shouldRefresh = remainingTime < 86400; // 24시간 (86400초)

            if (shouldRefresh) {
                log.info("액세스 토큰 갱신 필요: {}초 남음, 사용자: {}", remainingTime, email);
                
                // 새로운 토큰 생성
                String newAccessToken = jwtTokenProvider.refreshAccessToken(email);
                String newRefreshToken = jwtTokenProvider.createRefreshToken(email);

                TokenRefreshResponse response = TokenRefreshResponse.builder()
                        .accessToken(newAccessToken)
                        .refreshToken(newRefreshToken)
                        .tokenType("Bearer")
                        .expiresIn(jwtTokenProvider.getAccessTokenExpirationTime())
                        .build();

                return ResponseEntity.ok(new ApiResponse<>(true, "토큰이 갱신되었습니다.", response));
            } else {
                log.info("액세스 토큰이 아직 유효함: {}초 남음, 사용자: {}", remainingTime, email);
                
                // 기존 토큰 반환
                TokenRefreshResponse response = TokenRefreshResponse.builder()
                        .accessToken(request.getAccessToken())
                        .refreshToken(request.getRefreshToken())
                        .tokenType("Bearer")
                        .expiresIn(remainingTime)
                        .build();

                return ResponseEntity.ok(new ApiResponse<>(true, "토큰이 유효합니다.", response));
            }

        } catch (Exception e) {
            log.error("토큰 상태 확인 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(new ApiResponse<>(false, "토큰 상태 확인에 실패했습니다: " + e.getMessage(), null));
        }
    }
} 