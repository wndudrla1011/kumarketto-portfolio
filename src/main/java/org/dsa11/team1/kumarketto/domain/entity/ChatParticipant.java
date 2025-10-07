// org.dsa11.team1.kumarketto.domain.entity.ChatParticipant

package org.dsa11.team1.kumarketto.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.dsa11.team1.kumarketto.domain.enums.ChatStatus;

import java.time.LocalDateTime;
import java.util.Objects;

// @Data를 제거하고 아래 어노테이션들로 대체합니다.
@Getter
@Setter
@ToString(exclude = {"chatRoom", "memberEntity"}) // LazyInitializationException 방지를 위해 관계 필드는 toString에서 제외
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chat_participant")
public class ChatParticipant {

    @EmbeddedId
    private ChatParticipantId chatParticipantId;

    @MapsId("chatId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false, insertable = false, updatable = false)
    private ChatRoom chatRoom;

    @MapsId("userNo")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_no", nullable = false, insertable = false, updatable = false)
    private MemberEntity memberEntity;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "chat_status", nullable = false)
    private ChatStatus chatStatus = ChatStatus.ACTIVE;

    // --- [핵심 수정] ---
    // equals와 hashCode를 ID 필드인 chatParticipantId 기준으로만 구현합니다.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatParticipant that = (ChatParticipant) o;
        return Objects.equals(chatParticipantId, that.chatParticipantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chatParticipantId);
    }
}