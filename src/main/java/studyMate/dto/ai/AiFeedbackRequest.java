package studyMate.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiFeedbackRequest {
    private Long timerId;
    private String studySummary;
    private int studyTime;
    private int restTime;
    private String mode;
    
    // 추가 정보들
    private String studyTopic;        // 학습 주제 (예: "스프링 부트", "알고리즘")
    private String studyGoal;         // 학습 목표 (예: "JPA 마스터하기", "기초 문법 완성")
    private String difficulty;        // 학습 난이도 (예: "쉬움", "보통", "어려움")
    private String concentration;     // 집중도 (예: "높음", "보통", "낮음")
    private String mood;              // 학습 기분 (예: "좋음", "보통", "나쁨")
    private String interruptions;     // 방해 요소 (예: "없음", "휴대폰", "소음")
    private String studyMethod;       // 학습 방법 (예: "독서", "실습", "강의 시청")
    private String environment;       // 학습 환경 (예: "집", "도서관", "카페")
    private String energyLevel;       // 에너지 레벨 (예: "높음", "보통", "낮음")
    private String stressLevel;       // 스트레스 레벨 (예: "높음", "보통", "낮음")
} 