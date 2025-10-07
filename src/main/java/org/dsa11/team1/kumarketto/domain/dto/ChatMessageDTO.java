package org.dsa11.team1.kumarketto.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dsa11.team1.kumarketto.domain.enums.MessageType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {
    private Long messageId;
    private Long chatId;
    private String senderId;
    private String content; // TEXT 메시지
    private String imageUrl; // IMAGE 메시지
    private MessageType messageType; // TEXT / IMAGE
    private LocalDateTime createdDate;
}
