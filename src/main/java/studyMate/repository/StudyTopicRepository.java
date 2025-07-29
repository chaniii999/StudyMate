package studyMate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import studyMate.entity.StudyTopic;

@Repository
public interface StudyTopicRepository extends JpaRepository<StudyTopic, String> {

    // 추가적인 메소드가 필요하다면 여기에 정의할 수 있습니다.
    // 예: 특정 사용자에 대한 스터디 주제 조회 등
}
