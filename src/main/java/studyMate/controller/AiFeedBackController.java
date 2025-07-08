package studyMate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import studyMate.dto.ApiResponse;
import studyMate.dto.ai.AiFeedbackRequest;
import studyMate.dto.ai.AiFeedbackResponse;
import studyMate.service.AiFeedbackService;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiFeedBackController {
    private final AiFeedbackService aiFeedbackService;

    @PostMapping("/feedback")
    public ResponseEntity<ApiResponse<AiFeedbackResponse>> getFeedback(@RequestBody AiFeedbackRequest request) {
        try {
            AiFeedbackResponse feedback = aiFeedbackService.getFeedback(request);
            return ResponseEntity.ok(new ApiResponse<>(true, "AI 피드백이 성공적으로 생성되었습니다.", feedback));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "AI 피드백 생성에 실패했습니다: " + e.getMessage(), null));
        }
    }

    @GetMapping("/test")
    public ResponseEntity<ApiResponse<String>> testConnection() {
        return ResponseEntity.ok(new ApiResponse<>(true, "AI 피드백 서비스가 정상적으로 작동합니다.", "서비스 정상"));
    }

    @GetMapping("/feedback/{timerId}")
    public ResponseEntity<ApiResponse<AiFeedbackResponse>> getExistingFeedback(@PathVariable Long timerId) {
        try {
            AiFeedbackResponse feedback = aiFeedbackService.getExistingFeedback(timerId);
            return ResponseEntity.ok(new ApiResponse<>(true, "기존 AI 피드백을 성공적으로 조회했습니다.", feedback));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "AI 피드백 조회에 실패했습니다: " + e.getMessage(), null));
        }
    }
}
