package studyMate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import studyMate.config.OpenAiProrperties;
import studyMate.dto.ai.*;
import studyMate.entity.Timer;
import studyMate.repository.TimerRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiFeedbackService {
    private final WebClient openAiWebClient;
    private final OpenAiProrperties openAiProrperties;
    private final TimerRepository timerRepository;

    public AiFeedbackResponse getFeedback(AiFeedbackRequest request) {
        try {
            // Timer 데이터 조회
            Timer timer = timerRepository.findById(request.getTimerId())
                    .orElseThrow(() -> new RuntimeException("Timer not found"));

            // AI 피드백 요청 생성
            String prompt = createFeedbackPrompt(timer, request);
            
            OpenAiRequest openAiRequest = OpenAiRequest.builder()
                    .model("gpt-3.5-turbo")
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

            // OpenAI API 호출
            OpenAiResponse response = openAiWebClient.post()
                    .uri("/chat/completions")
                    .bodyValue(openAiRequest)
                    .retrieve()
                    .bodyToMono(OpenAiResponse.class)
                    .block();

            if (response != null && !response.getChoices().isEmpty()) {
                String aiResponse = response.getChoices().get(0).getMessage().getContent();
                AiFeedbackResponse feedbackResponse = parseAiResponse(aiResponse);
                
                // AI 피드백 결과를 Timer 엔티티에 저장
                timer.setAiFeedback(feedbackResponse.getFeedback());
                timer.setAiSuggestions(feedbackResponse.getSuggestions());
                timer.setAiMotivation(feedbackResponse.getMotivation());
                timer.setAiFeedbackCreatedAt(LocalDateTime.now());
                timerRepository.save(timer);
                
                return feedbackResponse;
            }

            throw new RuntimeException("AI 응답을 받지 못했습니다.");

        } catch (Exception e) {
            log.error("AI 피드백 생성 중 오류 발생", e);
            throw new RuntimeException("AI 피드백 생성에 실패했습니다: " + e.getMessage());
        }
    }

    private String createFeedbackPrompt(Timer timer, AiFeedbackRequest request) {
        return String.format("""
                다음 학습 기록을 바탕으로 피드백을 제공해주세요:
                
                학습 시간: %d분
                휴식 시간: %d분
                학습 모드: %s
                학습 요약: %s
                
                다음 형식으로 JSON 형태로 응답해주세요:
                {
                    "feedback": "전반적인 학습에 대한 피드백",
                    "suggestions": "개선 방안 제안",
                    "motivation": "동기부여 메시지"
                }
                """,
                timer.getStudyTime(),
                timer.getRestTime(),
                timer.getMode(),
                timer.getSummary() != null ? timer.getSummary() : "요약 없음"
        );
    }

    private AiFeedbackResponse parseAiResponse(String aiResponse) {
        try {
            // 간단한 파싱 (실제로는 JSON 파서 사용 권장)
            String feedback = extractSection(aiResponse, "feedback");
            String suggestions = extractSection(aiResponse, "suggestions");
            String motivation = extractSection(aiResponse, "motivation");

            return AiFeedbackResponse.builder()
                    .feedback(feedback)
                    .suggestions(suggestions)
                    .motivation(motivation)
                    .build();
        } catch (Exception e) {
            // 파싱 실패 시 전체 응답을 feedback으로 사용
            return AiFeedbackResponse.builder()
                    .feedback(aiResponse)
                    .suggestions("AI 응답을 파싱할 수 없습니다.")
                    .motivation("계속해서 학습을 진행해주세요!")
                    .build();
        }
    }

    private String extractSection(String response, String section) {
        // 간단한 텍스트 추출 (실제 구현에서는 JSON 파서 사용 권장)
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
                .orElseThrow(() -> new RuntimeException("Timer not found"));

        if (timer.getAiFeedback() == null) {
            throw new RuntimeException("AI 피드백이 아직 생성되지 않았습니다.");
        }

        return AiFeedbackResponse.builder()
                .feedback(timer.getAiFeedback())
                .suggestions(timer.getAiSuggestions())
                .motivation(timer.getAiMotivation())
                .build();
    }
} 