package studyMate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import studyMate.entity.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    // 이메일 중복 확인을 위한 메소드
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);
}
