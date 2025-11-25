# StudyMate

StudyMate는 Pomodoro 타이머와 AI 기반 피드백을 제공하는 학습 관리 플랫폼입니다. JWT 인증과 이메일 인증을 통해 안전하게 로그인하고, 실시간 타이머와 학습 목표 관리를 통해 효율적인 학습을 지원합니다.

## 주요 기능 ✨

### 🔐 인증 및 보안
- **JWT 기반 인증**: Access Token과 Refresh Token을 통한 안전한 인증 시스템
- **이메일 인증**: 회원가입 시 이메일 인증 코드 발송 및 검증
- **Spring Security**: 보안 필터 체인을 통한 엔드포인트 보호
- **Rate Limiting**: AI API 호출 제한을 통한 비용 관리

### ⏱️ 실시간 Pomodoro 타이머
- **WebSocket 기반 실시간 통신**: STOMP 프로토콜을 사용한 양방향 통신
- **타이머 상태 관리**: 시작, 일시정지, 정지, 전환 기능
- **학습 시간 기록**: 실제 학습 시간과 휴식 시간을 자동으로 기록
- **다양한 모드 지원**: 25/5, 50/10 등 다양한 Pomodoro 모드

### 📊 학습 목표 관리
- **목표 설정**: 목표명, 과목, 목표 시간, 목표 세션 수 설정
- **진행도 추적**: 현재 진행 시간, 세션 수를 실시간으로 추적
- **상태 관리**: ACTIVE, COMPLETED, PAUSED, CANCELLED 상태 관리
- **목표별 통계**: 목표별 학습 시간 및 세션 통계 제공

### 📅 스케줄 관리
- **일정 생성**: 날짜, 시간, 색상, 설명 등 상세한 일정 정보 관리
- **반복 일정**: DAILY, WEEKLY, MONTHLY 반복 규칙 지원
- **상태 추적**: PLANNED, IN_PROGRESS, COMPLETED, CANCELLED, POSTPONED 상태 관리
- **완료율 계산**: 계획된 학습 시간 대비 실제 학습 시간 기반 완료율 계산

### 🤖 AI 기반 피드백
- **OpenAI API 연동**: GPT 모델을 활용한 맞춤형 학습 피드백
- **학습 패턴 분석**: 타이머 기록과 사용자 데이터를 기반으로 학습 패턴 분석
- **개인화된 조언**: 피드백, 개선 제안, 동기부여 메시지 제공
- **Rate Limiting**: 분당 요청 수 제한을 통한 API 비용 최적화

### 📈 통계 및 분석
- **학습 시간 통계**: 일별, 주별, 월별 학습 시간 통계
- **주제별 분석**: StudyTopic별 학습 시간 및 횟수 추적
- **목표 달성률**: 목표 대비 실제 진행률 계산

## 기술 스택

### Backend
- **Java 17**: 최신 LTS 버전
- **Spring Boot 3.3.6**: 엔터프라이즈급 애플리케이션 프레임워크
- **Spring Security**: 인증 및 인가 처리
- **Spring Data JPA**: 데이터베이스 접근 계층
- **Spring WebSocket**: 실시간 양방향 통신
- **Spring WebFlux**: 비동기 HTTP 클라이언트 (OpenAI API 호출)

### Database & Cache
- **MySQL**: 관계형 데이터베이스 (영구 데이터 저장)
- **Redis**: 인메모리 데이터베이스 (인증 코드, 세션 관리)

### Authentication & Security
- **JWT (JSON Web Token)**: Access Token 및 Refresh Token
- **JJWT 0.11.5**: JWT 생성 및 검증 라이브러리
- **BCrypt**: 비밀번호 암호화

### External Services
- **OpenAI API**: AI 피드백 생성
- **Spring Mail**: 이메일 인증 코드 발송

### Build & Test
- **Gradle**: 빌드 도구
- **JUnit 5**: 단위 테스트
- **Spring Boot Test**: 통합 테스트

### Utilities
- **Lombok**: 보일러플레이트 코드 제거
- **ULID Creator**: 고유 ID 생성
- **Spring Boot Actuator**: 애플리케이션 모니터링

## 아키텍처

### 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                         Client (React Native/Web)                │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             │ HTTPS / WebSocket
                             │
┌────────────────────────────▼────────────────────────────────────┐
│                    Spring Boot Application                       │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              Security Filter Chain                        │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │     JwtAuthenticationFilter                        │  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐ │
│  │  REST Controllers│  │ WebSocket        │  │  Interceptors│ │
│  │  - Auth          │  │ Controller       │  │  - JWT       │ │
│  │  - Timer         │  │ - Timer          │  │  Handshake   │ │
│  │  - StudyGoal     │  └──────────────────┘  └──────────────┘ │
│  │  - Schedule      │                                          │
│  │  - AI Feedback   │                                          │
│  │  - User          │                                          │
│  └────────┬─────────┘                                          │
│           │                                                     │
│  ┌────────▼─────────────────────────────────────────────────┐  │
│  │                    Service Layer                          │  │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │  │
│  │  │ Auth     │ │ Timer    │ │ StudyGoal│ │ Schedule │  │  │
│  │  │ Service  │ │ Service  │ │ Service  │ │ Service  │  │  │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘  │  │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐               │  │
│  │  │ AI       │ │ User     │ │ Rate     │               │  │
│  │  │ Feedback │ │ Service  │ │ Limiter  │               │  │
│  │  │ Service  │ └──────────┘ │ Service  │               │  │
│  │  └──────────┘               └──────────┘               │  │
│  └────────┬─────────────────────────────────────────────────┘  │
│           │                                                     │
│  ┌────────▼─────────────────────────────────────────────────┐  │
│  │                  Repository Layer                         │  │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │  │
│  │  │ User     │ │ Timer    │ │ StudyGoal│ │ Schedule │  │  │
│  │  │ Repo     │ │ Repo     │ │ Repo     │ │ Repo     │  │  │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘  │  │
│  └────────┬─────────────────────────────────────────────────┘  │
└───────────┼─────────────────────────────────────────────────────┘
            │
    ┌───────┴────────┐
    │                │
┌───▼────┐    ┌─────▼─────┐
│ MySQL  │    │  Redis    │
│        │    │           │
│ - Users│    │ - Auth    │
│ - Timers│   │   Codes   │
│ - Goals │   │ - Sessions│
│ - Schedules│ │ - Cache   │
└────────┘    └───────────┘
    │
    │
┌───▼──────────────┐
│  OpenAI API      │
│  (External)      │
└──────────────────┘
```

### 데이터 흐름

#### 1. 인증 흐름
```
Client → POST /api/auth/send-code
       → AuthService.sendCode()
       → Redis (인증 코드 저장, TTL: 3분)
       → Email 발송

Client → POST /api/auth/verify-code
       → AuthService.verifyCode()
       → Redis 검증
       → 인증 완료 상태 저장 (TTL: 30분)

Client → POST /api/auth/sign-up
       → AuthService.signUp()
       → User 저장 (MySQL)
       → JWT 토큰 발급

Client → POST /api/auth/sign-in
       → AuthService.signIn()
       → User 검증
       → JWT 토큰 발급
```

#### 2. 타이머 흐름 (WebSocket)
```
Client → WebSocket 연결 (/ws-timer)
       → JwtHandshakeInterceptor (인증 검증)
       → STOMP 연결 성공

Client → /app/timer/start
       → TimerWebSocketController.startTimer()
       → TimerService.startTimer()
       → Redis (타이머 상태 저장)
       → /topic/timer (브로드캐스트)

Client → /app/timer/stop
       → TimerService.stopTimer()
       → Timer 엔티티 저장 (MySQL)
       → User.totalStudyTime 업데이트
       → /topic/timer (브로드캐스트)
```

#### 3. AI 피드백 흐름
```
Client → POST /api/ai-feedback
       → AiFeedbackController.getFeedback()
       → AiFeedbackService.getFeedback()
       → RateLimiterService.canMakeRequest() (Rate Limit 체크)
       → WebClient (OpenAI API 호출)
       → 응답 파싱
       → Timer 엔티티 업데이트 (MySQL)
       → 응답 반환
```

### 엔티티 관계도

```
User (1) ────< (N) StudyTopic
  │
  │ (1) ────< (N) StudyGoal
  │
  │ (1) ────< (N) Schedule
  │
  │ (1) ────< (N) Timer
  │
  └──────────────┘

StudyGoal (1) ────< (N) Timer
Schedule (1) ────< (N) Timer
```

## 프로젝트 구조

```
StudyMate/
├── src/
│   ├── main/
│   │   ├── java/studyMate/
│   │   │   ├── config/              # 설정 클래스
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   ├── JwtProperties.java
│   │   │   │   ├── OpenAiProperties.java
│   │   │   │   ├── PasswordEncoderConfig.java
│   │   │   │   ├── RedisConfig.java
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── WebClientConfig.java
│   │   │   │   └── WebSocketConfig.java
│   │   │   │
│   │   │   ├── controller/           # REST 컨트롤러
│   │   │   │   ├── AiFeedBackController.java
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── ScheduleController.java
│   │   │   │   ├── StudyGoalController.java
│   │   │   │   ├── TimerController.java
│   │   │   │   ├── TimerWebSocketController.java
│   │   │   │   ├── TokenController.java
│   │   │   │   └── UserController.java
│   │   │   │
│   │   │   ├── dto/                  # 데이터 전송 객체
│   │   │   │   ├── ai/
│   │   │   │   ├── auth/
│   │   │   │   ├── pomodoro/
│   │   │   │   ├── schedule/
│   │   │   │   ├── studygoal/
│   │   │   │   ├── timer/
│   │   │   │   ├── ApiResponse.java
│   │   │   │   └── TokenDto.java
│   │   │   │
│   │   │   ├── entity/               # JPA 엔티티
│   │   │   │   ├── GoalStatus.java
│   │   │   │   ├── Schedule.java
│   │   │   │   ├── StudyGoal.java
│   │   │   │   ├── StudyTopic.java
│   │   │   │   ├── Timer.java
│   │   │   │   └── User.java
│   │   │   │
│   │   │   ├── exception/            # 예외 처리
│   │   │   │   └── [14개의 커스텀 예외 클래스]
│   │   │   │
│   │   │   ├── interceptor/          # 인터셉터
│   │   │   │   └── JwtHandshakeInterceptor.java
│   │   │   │
│   │   │   ├── repository/           # JPA 리포지토리
│   │   │   │   ├── ScheduleRepository.java
│   │   │   │   ├── StudyGoalRepository.java
│   │   │   │   ├── StudyTopicRepository.java
│   │   │   │   ├── TimerRepository.java
│   │   │   │   └── UserRepository.java
│   │   │   │
│   │   │   ├── service/              # 비즈니스 로직
│   │   │   │   ├── AiFeedbackService.java
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   ├── RateLimiterService.java
│   │   │   │   ├── RedisService.java
│   │   │   │   ├── ScheduleService.java
│   │   │   │   ├── StudyGoalService.java
│   │   │   │   ├── TimerService.java
│   │   │   │   ├── TimerStatus.java
│   │   │   │   └── UserService.java
│   │   │   │
│   │   │   └── StudyMateApplication.java
│   │   │
│   │   └── resources/
│   │       └── application.yml       # 설정 파일
│   │
│   └── test/                         # 테스트 코드
│       └── java/studyMate/
│           └── service/
│               └── [9개의 서비스 테스트 클래스]
│
├── docs/                             # 문서
│   ├── CODE_REVIEW_GUIDELINE.md
│   ├── SERVICE_METHODS_GUIDE.md
│   └── UNIT_TEST_GUIDELINE.md
│
├── build.gradle                      # Gradle 빌드 설정
├── settings.gradle
└── README.md
```

## 주요 API 엔드포인트

### 인증 (Auth)
- `POST /api/auth/send-code` - 이메일 인증 코드 발송
- `POST /api/auth/verify-code` - 인증 코드 검증
- `POST /api/auth/sign-up` - 회원가입
- `POST /api/auth/sign-in` - 로그인
- `POST /api/auth/refresh` - 토큰 갱신

### 타이머 (Timer)
- `GET /api/timer/stats` - 타이머 통계 조회
- `GET /api/timer/history` - 타이머 기록 조회
- `POST /api/timer` - 타이머 기록 생성

### WebSocket (Timer)
- `WS /ws-timer` - WebSocket 연결
- `/app/timer/start` - 타이머 시작
- `/app/timer/stop` - 타이머 정지
- `/app/timer/pause` - 타이머 일시정지
- `/app/timer/switch` - 타이머 전환 (학습/휴식)
- `/topic/timer` - 타이머 상태 구독

### 학습 목표 (StudyGoal)
- `GET /api/study-goals` - 목표 목록 조회
- `POST /api/study-goals` - 목표 생성
- `GET /api/study-goals/{id}` - 목표 상세 조회
- `PUT /api/study-goals/{id}` - 목표 수정
- `DELETE /api/study-goals/{id}` - 목표 삭제

### 스케줄 (Schedule)
- `GET /api/schedules` - 스케줄 목록 조회
- `POST /api/schedules` - 스케줄 생성
- `GET /api/schedules/{id}` - 스케줄 상세 조회
- `PUT /api/schedules/{id}` - 스케줄 수정
- `DELETE /api/schedules/{id}` - 스케줄 삭제

### AI 피드백 (AI Feedback)
- `POST /api/ai-feedback` - AI 피드백 생성
- `GET /api/ai-feedback/{timerId}` - AI 피드백 조회

### 사용자 (User)
- `GET /api/user/profile` - 사용자 프로필 조회
- `PUT /api/user/profile` - 사용자 프로필 수정

## 데이터베이스 스키마

### 주요 테이블

#### users
- `id` (ULID, PK): 사용자 고유 ID
- `email` (VARCHAR(40), UNIQUE): 이메일
- `password` (VARCHAR(255)): 암호화된 비밀번호
- `nickname` (VARCHAR(15)): 닉네임
- `age` (INT): 나이
- `sex` (VARCHAR): 성별
- `total_study_time` (INT): 총 학습 시간 (분)
- `created_at`, `updated_at`: 타임스탬프

#### study_goals
- `id` (BIGINT, PK): 목표 ID
- `user_id` (FK): 사용자 ID
- `title` (VARCHAR(100)): 목표명
- `subject` (VARCHAR(50)): 과목
- `description` (TEXT): 상세 설명
- `color` (VARCHAR(7)): 색상 테마
- `start_date`, `target_date` (DATE): 시작일, 목표일
- `target_hours`, `target_sessions` (INT): 목표 시간, 세션 수
- `status` (ENUM): ACTIVE, COMPLETED, PAUSED, CANCELLED
- `current_hours`, `current_minutes`, `current_sessions` (INT): 현재 진행도

#### schedules
- `id` (ULID, PK): 스케줄 ID
- `user_id` (FK): 사용자 ID
- `topic_id` (FK): 학습 주제 ID
- `title` (VARCHAR(255)): 일정 제목
- `schedule_date` (DATE): 일정 날짜
- `start_time`, `end_time` (TIME): 시작/종료 시간
- `is_all_day`, `is_recurring` (BOOLEAN): 종일/반복 여부
- `recurrence_rule` (VARCHAR(20)): 반복 규칙
- `status` (ENUM): PLANNED, IN_PROGRESS, COMPLETED, CANCELLED, POSTPONED
- `completion_rate` (INT): 완료율

#### timers
- `id` (BIGINT, PK): 타이머 ID
- `user_id` (FK): 사용자 ID
- `study_goal_id` (FK): 학습 목표 ID (선택)
- `schedule_id` (FK): 스케줄 ID (선택)
- `start_time`, `end_time` (DATETIME): 시작/종료 시간
- `study_seconds`, `rest_seconds` (INT): 학습/휴식 시간 (초)
- `mode` (VARCHAR(20)): 타이머 모드
- `summary`, `ai_feedback`, `ai_suggestions`, `ai_motivation` (TEXT): AI 관련 필드

#### study_topics
- `id` (ULID, PK): 주제 ID
- `user_id` (FK): 사용자 ID
- `name` (VARCHAR(50)): 주제명
- `goal` (VARCHAR(255)): 목표
- `total_study_time` (INT): 총 학습 시간 (분)
- `total_study_count` (INT): 학습 횟수
- `strategy`, `summary` (TEXT): 전략 및 요약

## 빌드 및 실행

### 사전 요구사항
- Java 17 이상
- MySQL 8.0 이상
- Redis 6.0 이상
- Gradle 7.0 이상

### 환경 설정

1. **application.yml 설정**
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/studymate
    username: your_username
    password: your_password
  redis:
    host: localhost
    port: 6379
  mail:
    host: smtp.gmail.com
    port: 587
    username: your_email@gmail.com
    password: your_app_password

jwt:
  secret: your_jwt_secret_key
  access-token-validity: 3600000  # 1시간
  refresh-token-validity: 86400000 # 24시간

openai:
  api-key: your_openai_api_key
  model: gpt-4
  rate-limit:
    requests-per-minute: 3
```

2. **데이터베이스 생성**
```sql
CREATE DATABASE studymate CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 빌드

```bash
# 프로젝트 빌드
./gradlew build

# 테스트 제외 빌드
./gradlew build -x test
```

### 실행

```bash
# 애플리케이션 실행
./gradlew bootRun

# 또는 JAR 파일 실행
java -jar build/libs/StudyMate-0.0.1-SNAPSHOT.jar
```

### 테스트

```bash
# 모든 테스트 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests "studyMate.service.AuthServiceTest"

# 테스트 리포트 확인
# build/reports/tests/test/index.html
```

## 주요 설계 패턴 및 원칙

### 1. 계층형 아키텍처 (Layered Architecture)
- **Controller Layer**: HTTP 요청/응답 처리
- **Service Layer**: 비즈니스 로직 처리
- **Repository Layer**: 데이터 접근 처리

### 2. DTO 패턴
- 엔티티와 DTO 분리로 데이터 전송 계층과 영속성 계층 분리
- 요청/응답 DTO를 별도로 관리

### 3. 예외 처리
- 커스텀 예외 클래스를 통한 명확한 에러 메시지 제공
- 전역 예외 핸들러를 통한 일관된 에러 응답

### 4. 보안
- JWT 기반 Stateless 인증
- Spring Security를 통한 엔드포인트 보호
- 비밀번호 BCrypt 암호화

### 5. Rate Limiting
- AI API 호출 제한을 통한 비용 관리
- ConcurrentLinkedQueue를 사용한 분당 요청 수 제한

### 6. WebSocket 인증
- Handshake Interceptor를 통한 WebSocket 연결 시 인증 검증
- JWT 토큰을 통한 사용자 인증

## 기능 설명

### 1. 타이머 기록 📊
Pomodoro 세션이 끝날 때마다 사용 시간을 저장하고 통계로 확인할 수 있습니다.

<img width="381" height="809" alt="타이머 기록" src="https://github.com/user-attachments/assets/5ac74039-789f-4975-8e37-f4294f905c9f" />

### 2. AI 설문조사 데이터 조사 📝
간단한 설문을 통해 학습 습관을 분석하고 개인화된 데이터를 제공합니다.

<img width="381" height="809" alt="AI 설문조사" src="https://github.com/user-attachments/assets/be85c8bb-1585-4f12-a962-f3320e3b9a5c" />

### 3. AI 피드백 전달 🤖
축적된 데이터를 바탕으로 맞춤형 학습 팁을 제공합니다.

<img width="381" height="809" alt="AI 피드백" src="https://github.com/user-attachments/assets/28e4a8dc-dc94-4b4e-a34b-0514ac59620e" />

