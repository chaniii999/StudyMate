# StudyMate

StudyMate는 Pomodoro 타이머와 AI 기반 피드백을 제공하는 학습 관리 앱입니다. JWT와 이메일 인증으로 안전하게 로그인하고, 타이머 기록을 바탕으로 개인별 통계를 만들어 줍니다.

## 주요 기능 ✨
- ⏱️ **효율적인 타이머** - 공부와 휴식 시간을 번갈아 설정해 집중력을 유지해요.
- 📧 **안전한 로그인** - JWT 토큰과 이메일 인증으로 계정을 보호합니다.
- 🤖 **AI 피드백** - OpenAI와 연동해 학습 내용을 요약하고 맞춤 조언을 제공합니다.
- 📊 **기록 관리** - 타이머 사용 내역을 데이터베이스에 저장해 학습 패턴을 분석할 수 있습니다.

## 기술 스택
- Spring Boot, Spring Security
- JPA (MySQL)
- Redis
- WebSocket
- OpenAI API
- Lombok

## 빌드 및 테스트

```bash
./gradlew test
```

Gradle을 사용해 JUnit 테스트를 실행할 수 있습니다.
