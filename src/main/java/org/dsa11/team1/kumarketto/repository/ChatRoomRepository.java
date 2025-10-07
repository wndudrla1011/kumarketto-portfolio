// org.dsa11.team1.kumarketto.repository.ChatRoomRepository

package org.dsa11.team1.kumarketto.repository;

import org.dsa11.team1.kumarketto.domain.entity.ChatMessage;
import org.dsa11.team1.kumarketto.domain.entity.ChatRoom;
import org.dsa11.team1.kumarketto.domain.entity.MemberEntity;
import org.dsa11.team1.kumarketto.domain.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /**
     * 특정 상품에 대해 특정 두 명의 사용자가 모두 참여하고 있는 채팅방을 찾는 쿼리입니다.
     * JPQL을 사용하여 ChatRoom 엔티티와 ChatParticipant 엔티티를 조인합니다.
     *
     * @param product   채팅방이 연결된 상품
     * @param user1     참여자 1
     * @param user2     참여자 2
     * @return          조건을 만족하는 ChatRoom 리스트
     */
    //조건 1 : 상품이 일치해야하고
    // 조건2: 참여자 목록에 첫 번째 사람(판매자)이 존재하며
    // 조건3: 참여자 목록에 두 번째 사람(구매자)도 존재한다
    //상품이 일치하고 참여자 목록에 userid로 써 판매자 구매자ㅏ 모두 포함된 채팅방을 찾아주세요
    @Query("SELECT cr FROM ChatRoom cr " +
            "WHERE cr.product = :product " +
            "AND EXISTS (SELECT 1 FROM ChatParticipant cp WHERE cp.chatRoom = cr AND cp.memberEntity = :user1) " +
            "AND EXISTS (SELECT 1 FROM ChatParticipant cp WHERE cp.chatRoom = cr AND cp.memberEntity = :user2)")
    List<ChatRoom> findChatRoomByProductAndUsers(@Param("product") Product product,
                                                 @Param("user1") MemberEntity user1,
                                                 @Param("user2") MemberEntity user2);

    /**
     * chatId로 ChatRoom을 조회할 때, participants 컬렉션을 함께 fetch join하여
     * LazyInitializationException을 방지합니다.
     * @param chatId 채팅방 ID
     * @return ChatRoom 객체를 담은 Optional
     */
    @Query("SELECT cr FROM ChatRoom cr JOIN FETCH cr.participants WHERE cr.chatId = :chatId")
    Optional<ChatRoom> findByIdWithParticipants(@Param("chatId") Long chatId);

    /**
     * chatId로 ChatRoom을 조회할 때, participants와 각 participant의 memberEntity까지
     * 모두 fetch join하여 LazyInitializationException을 완벽하게 방지합니다.
     * @param chatId 채팅방 ID
     * @return ChatRoom 객체를 담은 Optional
     */
    @Query("SELECT cr FROM ChatRoom cr " +
            "JOIN FETCH cr.participants p " +      // 1. 참여자 목록(participants)을 함께 가져온다 (p라는 별칭 부여)
            "JOIN FETCH p.memberEntity " +        // 2. 각 참여자(p)의 회원 상세정보(memberEntity)까지 함께 가져온다
            "WHERE cr.chatId = :chatId")
    Optional<ChatRoom> findByIdWithParticipantsAndMembers(@Param("chatId") Long chatId);


}