// org.dsa11.team1.kumarketto.domain.dto.ChatMessageSendDTO.java

package org.dsa11.team1.kumarketto.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dsa11.team1.kumarketto.domain.entity.ChatMessage;
import org.dsa11.team1.kumarketto.domain.enums.MessageType;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageSendDTO {
    // 프론트엔드에 실제로 필요한 데이터만 정의합니다.
    private Long messageId;
    private Long chatId;
    private String senderId;
    private String content;
    private String imageUrl;
    private MessageType messageType;
    private LocalDateTime createdDate;
    private boolean isRead;

    // 엔티티 객체를 이 DTO로 쉽게 변환하기 위한 생성자 또는 메소드를 만듭니다.
    public static ChatMessageSendDTO fromEntity(ChatMessage entity) {
        return ChatMessageSendDTO.builder()
                .messageId(entity.getMessageId())
                .chatId(entity.getChatRoom().getChatId())
                .senderId(entity.getSender().getUserId())
                .content(entity.getContent())
                .imageUrl(entity.getImageUrl())
                .messageType(entity.getMessageType())
                .createdDate(entity.getCreatedDate())
                .isRead(entity.isRead())
                .build();
    }
}