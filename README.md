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

## 기능 설명
1. **타이머 기록** 📊 - Pomodoro 세션이 끝날 때마다 사용 시간을 저장하고 통계로 확인할 수 있습니다.
  <img width="381" height="809" alt="1122" src="https://github.com/user-attachments/assets/5ac74039-789f-4975-8e37-f4294f905c9f" />

2. **AI 설문조사 데이터 조사** 📝 - 간단한 설문을 통해 학습 습관을 분석하고 개인화된 데이터를 제공합니다.
   <img width="381" height="809" alt="111" src="https://github.com/user-attachments/assets/be85c8bb-1585-4f12-a962-f3320e3b9a5c" />

4. **AI 피드백 전달** 🤖 - 축적된 데이터를 바탕으로 맞춤형 학습 팁을 제공합니다.
   <img width="381" height="809" alt="133" src="https://github.com/user-attachments/assets/28e4a8dc-dc94-4b4e-a34b-0514ac59620e" />


![타이머 화면](images/timer.png)
![설문 화면](images/survey.png)
![피드백 화면](images/feedback.png)

## 빌드 및 테스트

```bash
./gradlew test
```

Gradle을 사용해 JUnit 테스트를 실행할 수 있습니다.
