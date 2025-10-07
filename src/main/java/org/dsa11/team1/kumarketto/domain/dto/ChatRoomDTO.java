package org.dsa11.team1.kumarketto.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomDTO {
    private Long chatId;
    private Long productId;
    private LocalDateTime createdAt;
    private LocalDateTime lastMessageAt;
    private List<ChatParticipantDTO> participants;
}
