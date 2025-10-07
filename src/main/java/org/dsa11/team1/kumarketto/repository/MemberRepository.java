package org.dsa11.team1.kumarketto.repository;

import org.dsa11.team1.kumarketto.domain.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<MemberEntity, Long> {

    @Query("SELECT m.userId FROM MemberEntity m")
    List<String> findAllUserIds();

    Optional<MemberEntity> findByUserId(String userId);
    Optional<MemberEntity> findByEmail(String userEmail);
    boolean existsByUserIdAndBirthDate(String userId, LocalDate birthDate);
    boolean existsByUserIdAndEnabledTrue(String userId);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
}
