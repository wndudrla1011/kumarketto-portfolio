package org.dsa11.team1.kumarketto.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dsa11.team1.kumarketto.domain.enums.ChatStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatParticipantDTO {
    private Long chatId;
    private String userId;
    private LocalDateTime joinedAt;
    private ChatStatus chatStatus;
}
