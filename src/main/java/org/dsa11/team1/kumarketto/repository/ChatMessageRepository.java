package org.dsa11.team1.kumarketto.repository;

import org.dsa11.team1.kumarketto.domain.entity.ChatMessage;
import org.dsa11.team1.kumarketto.domain.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 특정 채팅방(chatId)의 모든 메시지를 생성 시간(createdDate) 오름차순으로 조회
    //WHERE chat_room_id = ? (파라미터 chatId가 들어갑니다.)
    List<ChatMessage> findByChatRoom_ChatIdOrderByCreatedDateAsc(Long chatId);

    //  특정 채팅방에서 가장 최근 메시지 1개를 찾아 반환합니다.
    Optional<ChatMessage> findTopByChatRoomOrderByCreatedDateDesc(ChatRoom chatRoom);


    /**
     * 특정 채팅방에서, 특정 사용자가 보낸 것이 아닌 모든 메시지를 읽음 처리합니다.
     * @param chatId 채팅방 ID
     * @param recipientId 현재 사용자(메시지를 읽는 사람)의 ID
     */
    @Modifying //  @Modifying, @Transactional 어노테이션 추가
    @Transactional
    @Query("UPDATE ChatMessage cm SET cm.isRead = true " +
            "WHERE cm.chatRoom.chatId = :chatId AND cm.sender.userId != :recipientId AND cm.isRead = false")
    void markMessagesAsRead(@Param("chatId") Long chatId, @Param("recipientId") String recipientId);


    /**
     * 특정 채팅방에서, 특정 사용자 번호(userNo)를 가진 사람이 보낸 것이 아닌
     * 모든 안 읽은 메시지를 읽음 처리합니다.
     */
    // @Modifying 어노테이션에 (clearAutomatically = true) 옵션을 추가합니다.
    //@Modifying: 이 쿼리는 SELECT가 아닌 UPDATE, DELETE 등 데이터를 변경하는
    //쿼리임을 JPA에게 알려줍니다.
    @Modifying(clearAutomatically = true)
    @Transactional
    // @Query: 메소드 이름으로 쿼리를 만드는 대신, JPQL을 직접 작성합니다.
    //해당 채팅방에서 상대방이 보낸 모든 안 읽은 메시지들이 DB에서 is_read = true로 변경됩니다.
    @Query("UPDATE ChatMessage cm SET cm.isRead = true " +
            "WHERE cm.chatRoom.chatId = :chatId AND cm.sender.userNo <> :recipientUserNo AND cm.isRead = false")
    void markAllAsReadByRecipient(@Param("chatId") Long chatId, @Param("recipientUserNo") Long recipientUserNo);




    /**
     * 특정 채팅방에 현재 사용자가 보낸 것이 아닌 안 읽은 메시지가 있는지 확인합니다.
     * @param chatId 채팅방 ID
     * @param currentUserId 현재 사용자 ID
     * @return 존재하면 true, 아니면 false
     */
    //실제 쿼리문
    //SELECT COUNT(*)
    // FROM chat_message
    // WHERE chat_room_id = [chatId]
    // AND sender_user_id != [currentUserId]
    // AND is_read = false;
    //카운터 chat_message 테이블 채팅방중에서 내가 읽지 않은 메세지가 있는지 검색
    //안읽음 메세지를 표시를 위한것
    boolean existsByChatRoom_ChatIdAndSender_UserIdNotAndIsReadFalse(Long chatId, String currentUserId);


}
