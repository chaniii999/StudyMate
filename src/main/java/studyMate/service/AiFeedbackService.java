package studyMate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import studyMate.dto.ai.AiFeedbackRequest;
import studyMate.dto.ai.AiFeedbackResponse;
import studyMate.dto.ai.OpenAiRequest;
import studyMate.dto.ai.OpenAiResponse;
import studyMate.entity.Timer;
import studyMate.entity.User;
import studyMate.exception.AiServiceException;
import studyMate.exception.RateLimitExceededException;
import studyMate.exception.StudyMateException;
import studyMate.exception.StudyTimeTooShortException;
import studyMate.exception.TimerNotFoundException;
import studyMate.repository.TimerRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiFeedbackService {
    private final WebClient openAiWebClient;
    private final TimerRepository timerRepository;
    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiFeedbackResponse getFeedback(AiFeedbackRequest request) {
        try {
            // 1. Timer 데이터 조회 및 검증
            Timer timer = validateAndGetTimer(request);
            
            // 2. 학습 시간 검증
            validateStudyTime(timer);
            
            // 3. Rate Limit 확인
            checkRateLimit();
            
            // 4. 요청 데이터 로깅
            logRequestData(timer, request);
            
            // 5. AI 피드백 요청 생성
            OpenAiRequest openAiRequest = buildOpenAiRequest(timer, request);
            
            // 6. OpenAI API 호출 (재시도 로직 포함)
            OpenAiResponse response = callOpenAiWithRetry(openAiRequest);
            
            // 7. 응답 파싱 및 저장
            return parseAndSaveFeedback(timer, response, request);
            
        } catch (WebClientResponseException e) {
            throw handleWebClientResponseException(e);
        } catch (WebClientRequestException e) {
            log.error("OpenAI API 연결 오류: {}", e.getMessage());
            throw new AiServiceException("AI 서비스 연결에 실패했습니다. 네트워크 상태를 확인하고 잠시 후 다시 시도해주세요.", e);
        } catch (StudyMateException e) {
            // 커스텀 예외는 그대로 전파
            throw e;
        } catch (Exception e) {
            log.error("AI 피드백 생성 중 오류 발생", e);
            throw new AiServiceException("AI 피드백 생성에 실패했습니다: " + e.getMessage(), e);
        }
    }
    
    /**
     * Timer 데이터 조회 및 검증
     */
    private Timer validateAndGetTimer(AiFeedbackRequest request) {
        Timer timer = timerRepository.findById(request.getTimerId())
                .orElseThrow(() -> new TimerNotFoundException(request.getTimerId()));
        
        // 학습 요약이 없는 경우 경고
        String finalSummary = request.getStudySummary() != null ? request.getStudySummary() : 
                             (timer.getSummary() != null ? timer.getSummary() : "");
        if (finalSummary.trim().isEmpty()) {
            log.warn("AI 피드백 요청 경고: 학습 요약이 없습니다.");
        }
        
        // 학습 모드가 없는 경우 경고
        String finalMode = request.getMode() != null ? request.getMode() : timer.getMode();
        if (finalMode == null || finalMode.trim().isEmpty()) {
            log.warn("AI 피드백 요청 경고: 학습 모드 정보가 없습니다.");
        }
        
        return timer;
    }
    
    /**
     * 학습 시간 검증
     */
    private void validateStudyTime(Timer timer) {
        int studyTime = timer.getStudyTime();
        int minimumSeconds = 120; // 최소 2분
        
        if (studyTime < minimumSeconds) {
            log.warn("AI 피드백 요청 거부: 학습 시간이 너무 짧습니다. studyTime: {}초({}분)", 
                    studyTime, studyTime / 60);
            throw new StudyTimeTooShortException(studyTime, minimumSeconds);
        }
    }
    
    /**
     * Rate Limit 확인
     */
    private void checkRateLimit() {
        if (!rateLimiterService.canMakeRequest()) {
            int currentRequests = rateLimiterService.getCurrentRequestCount();
            int maxRequests = rateLimiterService.getMaxRequestsPerMinute();
            log.warn("Rate limit exceeded. Current requests: {}/{}", currentRequests, maxRequests);
            throw new RateLimitExceededException(currentRequests, maxRequests);
        }
    }
    
    /**
     * 요청 데이터 로깅
     */
    private void logRequestData(Timer timer, AiFeedbackRequest request) {
        log.info("Timer 데이터 - studyTime: {}초({}분), restTime: {}초({}분), mode: {}, summary: {}", 
                timer.getStudyTime(), timer.getStudyTime()/60, timer.getRestTime(), timer.getRestTime()/60, 
                timer.getMode(), timer.getSummary());
        log.info("Request 기본 데이터 - studyTime: {}, restTime: {}, mode: {}, summary: {}", 
                request.getStudyTime(), request.getRestTime(), request.getMode(), request.getStudySummary());
        log.info("Request 추가 데이터 - topic: {}, goal: {}, difficulty: {}, concentration: {}, mood: {}", 
                request.getStudyTopic(), request.getStudyGoal(), request.getDifficulty(), 
                request.getConcentration(), request.getMood());
        log.info("Request 환경 데이터 - interruptions: {}, method: {}, environment: {}, energy: {}, stress: {}", 
                request.getInterruptions(), request.getStudyMethod(), request.getEnvironment(), 
                request.getEnergyLevel(), request.getStressLevel());
    }
    
    /**
     * OpenAI 요청 생성
     */
    private OpenAiRequest buildOpenAiRequest(Timer timer, AiFeedbackRequest request) {
        String prompt = createFeedbackPrompt(timer, request);
        log.info("생성된 프롬프트: {}", prompt);
        
        return OpenAiRequest.builder()
                .model("gpt-4o-mini")
                .temperature(0.7)
                .messages(List.of(
                        OpenAiRequest.Message.builder()
                                .role("system")
                                .content("당신은 학습 효과를 분석하고 개선 방안을 제시하는 전문가입니다. 한국어로 답변해주세요.")
                                .build(),
                        OpenAiRequest.Message.builder()
                                .role("user")
                                .content(prompt)
                                .build()
                ))
                .build();
    }
    
    /**
     * OpenAI API 호출 (재시도 로직 포함)
     */
    private OpenAiResponse callOpenAiWithRetry(OpenAiRequest openAiRequest) {
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                return openAiWebClient.post()
                        .uri("/chat/completions")
                        .bodyValue(openAiRequest)
                        .retrieve()
                        .bodyToMono(OpenAiResponse.class)
                        .block();
            } catch (WebClientRequestException e) {
                retryCount++;
                log.warn("OpenAI API 연결 오류 (재시도 {}/{}): {}", retryCount, maxRetries, e.getMessage());
                
                if (retryCount >= maxRetries) {
                    throw e; // 최대 재시도 횟수 초과
                }
                
                // 재시도 전 잠시 대기 (지수 백오프)
                try {
                    Thread.sleep(1000L * retryCount); // 1초, 2초, 3초 대기
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new AiServiceException("재시도 대기 중 인터럽트 발생", ie);
                }
            }
        }
        
        throw new AiServiceException("AI 응답을 받지 못했습니다. 최대 재시도 횟수를 초과했습니다.");
    }
    
    /**
     * 응답 파싱 및 저장
     */
    private AiFeedbackResponse parseAndSaveFeedback(Timer timer, OpenAiResponse response, AiFeedbackRequest request) {
        if (response == null || response.getChoices().isEmpty()) {
            throw new AiServiceException("AI 응답을 받지 못했습니다.");
        }
        
        String aiResponse = response.getChoices().get(0).getMessage().getContent();
        AiFeedbackResponse feedbackResponse = parseAiResponse(aiResponse);
        
        // 요청 데이터를 정리해서 응답에 포함
        AiFeedbackResponse.StudySessionSummary sessionSummary = createSessionSummary(timer, request);
        feedbackResponse.setSessionSummary(sessionSummary);
        
        // AI 피드백 결과를 Timer 엔티티에 저장
        timer.updateAiFeedback(
                feedbackResponse.getFeedback(),
                feedbackResponse.getSuggestions(),
                feedbackResponse.getMotivation()
        );
        timerRepository.save(timer);
        
        return feedbackResponse;
    }
    
    /**
     * WebClientResponseException 처리
     */
    private AiServiceException handleWebClientResponseException(WebClientResponseException e) {
        log.error("OpenAI API 호출 실패 - HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
        
        int statusCode = e.getStatusCode().value();
        if (statusCode == 429) {
            return new AiServiceException("AI 서비스 사용량이 초과되었습니다. 잠시 후 다시 시도해주세요.");
        } else if (statusCode == 401) {
            return new AiServiceException("AI 서비스 인증에 실패했습니다. 관리자에게 문의해주세요.");
        } else {
            return new AiServiceException("AI 서비스에 일시적인 문제가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    private String createFeedbackPrompt(Timer timer, AiFeedbackRequest request) {
        // 기본 데이터 (초 단위로 저장된 데이터를 분으로 변환하여 표시)
        // 타이머에 저장된 값을 사용 (단일 진실 공급원)
        int studyTimeMinutes = timer.getStudyTime() / 60;
        int restTimeMinutes = timer.getRestTime() / 60;
        String mode = request.getMode() != null ? request.getMode() : timer.getMode();
        String summary = request.getStudySummary() != null ? request.getStudySummary() : 
                        (timer.getSummary() != null ? timer.getSummary() : "요약 없음");
        
        // 추가 정보들
        String studyTopic = request.getStudyTopic() != null ? request.getStudyTopic() : "정보 없음";
        String studyGoal = request.getStudyGoal() != null ? request.getStudyGoal() : "정보 없음";
        String difficulty = request.getDifficulty() != null ? request.getDifficulty() : "정보 없음";
        String concentration = request.getConcentration() != null ? request.getConcentration() : "정보 없음";
        String mood = request.getMood() != null ? request.getMood() : "정보 없음";
        String interruptions = request.getInterruptions() != null ? request.getInterruptions() : "정보 없음";
        String studyMethod = request.getStudyMethod() != null ? request.getStudyMethod() : "정보 없음";
        String environment = request.getEnvironment() != null ? request.getEnvironment() : "정보 없음";
        String energyLevel = request.getEnergyLevel() != null ? request.getEnergyLevel() : "정보 없음";
        String stressLevel = request.getStressLevel() != null ? request.getStressLevel() : "정보 없음";
        
        // 사용자 전체 학습 통계
        User user = timer.getUser();
        int userTotalStudyTime = user.getTotalStudyTime();
        
        return String.format("""
                다음 학습 기록을 바탕으로 종합적인 피드백을 제공해주세요:
                
                === 기본 학습 정보 ===
                학습 시간: %d분
                휴식 시간: %d분
                학습 모드: %s
                학습 요약: %s
                
                === 상세 학습 정보 ===
                학습 주제: %s
                학습 목표: %s
                학습 난이도: %s
                집중도: %s
                학습 기분: %s
                방해 요소: %s
                학습 방법: %s
                학습 환경: %s
                에너지 레벨: %s
                스트레스 레벨: %s
                
                === 사용자 전체 학습 통계 ===
                총 학습 시간: %d분
                
                === 분석 요청사항 ===
                1. 학습 효율성 분석 (시간 대비 집중도, 환경 요인 등)
                2. 개인적 요인 분석 (기분, 에너지, 스트레스가 학습에 미친 영향)
                3. 환경적 요인 분석 (학습 환경, 방해 요소의 영향)
                4. 학습 방법 분석 (사용한 방법의 적절성)
                5. 목표 달성도 평가 (목표 대비 진행 상황)
                6. 전체 학습 패턴 분석 (총 학습 시간 대비 이번 세션의 의미)
                7. 개선 가능성 평가 (현재 상황에서 개선할 수 있는 부분)
                
                다음 형식으로 JSON 형태로 응답해주세요:
                {
                    "feedback": "전반적인 학습에 대한 종합적인 피드백 (위의 분석 요청사항들을 포함)",
                    "suggestions": "구체적인 개선 방안 제안 (환경, 방법, 습관 등 다양한 측면에서)",
                    "motivation": "개인적 상황을 고려한 맞춤형 동기부여 메시지"
                }
                """,
                studyTimeMinutes, restTimeMinutes, mode, summary,
                studyTopic, studyGoal, difficulty, concentration, mood, 
                interruptions, studyMethod, environment, energyLevel, stressLevel,
                userTotalStudyTime
        );
    }

    /**
     * AI 응답 파싱 (JSON 우선, 실패 시 텍스트 파싱)
     */
    private AiFeedbackResponse parseAiResponse(String aiResponse) {
        try {
            // 1. JSON 파싱 시도
            return parseJsonResponse(aiResponse);
        } catch (JsonProcessingException e) {
            log.warn("JSON 파싱 실패, 텍스트 파싱 시도: {}", e.getMessage());
            // 2. JSON이 아닌 경우 텍스트 파싱 시도
            return parseTextResponse(aiResponse);
        }
    }
    
    /**
     * JSON 형식의 AI 응답 파싱
     */
    private AiFeedbackResponse parseJsonResponse(String aiResponse) throws JsonProcessingException {
        // JSON 코드 블록 제거 (```json ... ```)
        String cleanedResponse = aiResponse.replaceAll("```json\\s*", "")
                                           .replaceAll("```\\s*", "")
                                           .trim();
        
        JsonNode jsonNode = objectMapper.readTree(cleanedResponse);
        
        String feedback = jsonNode.has("feedback") ? 
                jsonNode.get("feedback").asText() : "피드백을 제공할 수 없습니다.";
        String suggestions = jsonNode.has("suggestions") ? 
                jsonNode.get("suggestions").asText() : "제안을 제공할 수 없습니다.";
        String motivation = jsonNode.has("motivation") ? 
                jsonNode.get("motivation").asText() : "계속해서 학습을 진행해주세요!";
        
        return AiFeedbackResponse.builder()
                .feedback(feedback)
                .suggestions(suggestions)
                .motivation(motivation)
                .build();
    }
    
    /**
     * 텍스트 형식의 AI 응답 파싱 (하위 호환성)
     */
    private AiFeedbackResponse parseTextResponse(String aiResponse) {
        try {
            String feedback = extractSection(aiResponse, "feedback");
            String suggestions = extractSection(aiResponse, "suggestions");
            String motivation = extractSection(aiResponse, "motivation");

            return AiFeedbackResponse.builder()
                    .feedback(feedback)
                    .suggestions(suggestions)
                    .motivation(motivation)
                    .build();
        } catch (Exception e) {
            log.warn("텍스트 파싱도 실패, 전체 응답을 feedback으로 사용: {}", e.getMessage());
            // 파싱 실패 시 전체 응답을 feedback으로 사용
            return AiFeedbackResponse.builder()
                    .feedback(aiResponse)
                    .suggestions("AI 응답을 파싱할 수 없습니다.")
                    .motivation("계속해서 학습을 진행해주세요!")
                    .build();
        }
    }
    
    /**
     * 텍스트에서 섹션 추출 (하위 호환성)
     */
    private String extractSection(String response, String section) {
        String lowerResponse = response.toLowerCase();
        String lowerSection = section.toLowerCase();
        
        int startIndex = lowerResponse.indexOf(lowerSection);
        if (startIndex == -1) {
            return "해당 섹션을 찾을 수 없습니다.";
        }
        
        startIndex = response.indexOf(":", startIndex);
        if (startIndex == -1) {
            return "해당 섹션을 찾을 수 없습니다.";
        }
        
        startIndex += 1;
        int endIndex = response.indexOf("\n", startIndex);
        if (endIndex == -1) {
            endIndex = response.length();
        }
        
        return response.substring(startIndex, endIndex).trim();
    }

    // 기존 AI 피드백 조회
    public AiFeedbackResponse getExistingFeedback(Long timerId) {
        Timer timer = timerRepository.findById(timerId)
                .orElseThrow(() -> new TimerNotFoundException(timerId));

        if (timer.getAiFeedback() == null) {
            throw new RuntimeException("AI 피드백이 아직 생성되지 않았습니다.");
        }

        // 기존 요청 데이터로 세션 요약 생성 (기본값 사용)
        AiFeedbackRequest defaultRequest = AiFeedbackRequest.builder()
                .timerId(timerId)
                .studyTime(timer.getStudyTime())
                .restTime(timer.getRestTime())
                .mode(timer.getMode())
                .studySummary(timer.getSummary())
                .build();
        
        AiFeedbackResponse.StudySessionSummary sessionSummary = createSessionSummary(timer, defaultRequest);

        return AiFeedbackResponse.builder()
                .sessionSummary(sessionSummary)
                .feedback(timer.getAiFeedback())
                .suggestions(timer.getAiSuggestions())
                .motivation(timer.getAiMotivation())
                .build();
    }

    // 요청 데이터를 정리해서 세션 요약 생성
    private AiFeedbackResponse.StudySessionSummary createSessionSummary(Timer timer, AiFeedbackRequest request) {
        // 기본 데이터 (초 단위)
        // 타이머에 저장된 값을 사용 (단일 진실 공급원)
        int studyTimeSeconds = timer.getStudyTime();
        int restTimeSeconds = timer.getRestTime();
        
        // 분 단위 변환
        int studyTimeMinutes = studyTimeSeconds / 60;
        int restTimeMinutes = restTimeSeconds / 60;
        
        // 모드와 요약
        String mode = request.getMode() != null ? request.getMode() : timer.getMode();
        String summary = request.getStudySummary() != null ? request.getStudySummary() : 
                        (timer.getSummary() != null ? timer.getSummary() : "");
        
        // 사용자 전체 학습 통계
        User user = timer.getUser();
        int userTotalStudyTimeMinutes = user.getTotalStudyTime();
        
        return AiFeedbackResponse.StudySessionSummary.builder()
                // 기본 정보
                .studyTimeSeconds(studyTimeSeconds)
                .studyTimeMinutes(studyTimeMinutes)
                .restTimeSeconds(restTimeSeconds)
                .restTimeMinutes(restTimeMinutes)
                .mode(mode != null ? mode : "정보 없음")
                .summary(summary)
                
                // 상세 정보
                .studyTopic(request.getStudyTopic() != null ? request.getStudyTopic() : "정보 없음")
                .studyGoal(request.getStudyGoal() != null ? request.getStudyGoal() : "정보 없음")
                .difficulty(request.getDifficulty() != null ? request.getDifficulty() : "정보 없음")
                .concentration(request.getConcentration() != null ? request.getConcentration() : "정보 없음")
                .mood(request.getMood() != null ? request.getMood() : "정보 없음")
                .interruptions(request.getInterruptions() != null ? request.getInterruptions() : "정보 없음")
                .studyMethod(request.getStudyMethod() != null ? request.getStudyMethod() : "정보 없음")
                .environment(request.getEnvironment() != null ? request.getEnvironment() : "정보 없음")
                .energyLevel(request.getEnergyLevel() != null ? request.getEnergyLevel() : "정보 없음")
                .stressLevel(request.getStressLevel() != null ? request.getStressLevel() : "정보 없음")
                
                // 사용자 통계
                .userTotalStudyTimeMinutes(userTotalStudyTimeMinutes)
                .build();
    }
} 