package org.dsa11.team1.kumarketto.repository;

import org.dsa11.team1.kumarketto.domain.entity.ChatParticipant;
import org.dsa11.team1.kumarketto.domain.entity.ChatParticipantId;
import org.dsa11.team1.kumarketto.domain.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, ChatParticipantId> {
    List<ChatParticipant> findByMemberEntity(MemberEntity memberEntity);

}
