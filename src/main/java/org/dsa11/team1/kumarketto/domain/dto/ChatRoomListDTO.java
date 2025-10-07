package org.dsa11.team1.kumarketto.domain.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ChatRoomListDTO {
    private Long chatId;
    private Long productId;
    private String productTitle;
    private Integer productPrice;
    private String productImageUrl;
    private String opponentNickname;
    private String lastMessageContent;
    private LocalDateTime lastMessageAt;
    private boolean hasUnreadMessages;
}