# StudyMate

StudyMate는 Pomodoro 타이머와 AI 피드백 기능을 제공하는 학습 관리 애플리케이션입니다. JWT 인증, 이메일 인증 및 타이머 기록 저장을 지원합니다.

## 주요 도메인 기술

- **JWT 기반 인증**: `JwtTokenProvider`에서 액세스/리프레시 토큰을 생성하고 검증합니다. `AuthService`가 이메일 인증 및 토큰 갱신을 담당합니다.
- **타이머 관리**: `TimerService`는 타이머 시작/정지/일시정지/모드 전환을 처리하며 `Timer` 엔티티에 학습 기록을 저장합니다.
- **AI 피드백**: `AiFeedbackService`가 OpenAI API를 호출하여 학습 요약을 분석하고 피드백을 제공합니다.

## 핵심 메서드 설명

### AuthService
- `sendCode(email)`: 이메일로 인증 코드를 전송합니다.
- `verifyCode(email, code)`: 입력한 코드가 맞는지 검증하고 인증 상태를 저장합니다.
- `refreshToken(refreshToken)`: 리프레시 토큰을 검증하고 새로운 액세스/리프레시 토큰을 발급합니다.

### TimerService
- `startTimer(user, dto)`: 사용자의 타이머를 시작합니다. 학습/휴식 모드와 지속 시간을 설정합니다.
- `pauseTimer(user)`: 실행 중인 타이머를 일시정지합니다.
- `stopTimer(user)`: 타이머를 완전히 중지하고 실제 학습 시간을 계산합니다.
- `switchTimer(user)`: 학습과 휴식 모드를 교대하며 새로운 사이클을 시작합니다.
- `saveTimerRecord(user, studySec, restSec, start, end, mode, summary)`: 타이머 기록을 데이터베이스에 저장합니다.

### AiFeedbackService
- `getFeedback(request)`: 타이머 기록과 사용자의 입력을 바탕으로 OpenAI API에 요청을 보내고 피드백을 생성합니다.
- `getExistingFeedback(timerId)`: 이미 저장된 AI 피드백을 조회합니다.

## 빌드 및 테스트

```bash
./gradlew test
```

Gradle을 사용해 JUnit 테스트를 실행할 수 있습니다.
