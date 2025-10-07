package org.dsa11.team1.kumarketto.domain.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ChatParticipantId implements Serializable {
    private Long chatId;
    private Long userNo;

    @Override
    public boolean equals(Object object) {
        if(this == object) return true;
        if(!(object instanceof ChatParticipantId)) return false;
        ChatParticipantId that = (ChatParticipantId) object;
        return Objects.equals(chatId, that.chatId) &&
               Objects.equals(userNo, that.userNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chatId, userNo);
    }
}
