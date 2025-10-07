// org.dsa11.team1.kumarketto.service.ChatRoomService

package org.dsa11.team1.kumarketto.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.dsa11.team1.kumarketto.domain.dto.ChatRoomDTO;
import org.dsa11.team1.kumarketto.domain.dto.ChatRoomListDTO;
import org.dsa11.team1.kumarketto.domain.entity.*;
import org.dsa11.team1.kumarketto.domain.enums.ChatStatus;
import org.dsa11.team1.kumarketto.domain.enums.MessageType;
import org.dsa11.team1.kumarketto.domain.enums.ProductStatus;
import org.dsa11.team1.kumarketto.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ObjectMapper objectMapper;

    /**
     * 상품 ID와 구매자 ID를 기반으로 채팅방을 찾거나 새로 생성합니다.
     * 1. 상품 정보를 조회하여 판매자를 찾습니다.
     * 2. 판매자와 구매자가 모두 참여하고 있는 해당 상품의 채팅방이 있는지 확인합니다.
     * 3. 채팅방이 존재하면 해당 채팅방 정보를 반환합니다.
     * 4. 채팅방이 존재하지 않으면 새로운 채팅방과 참여자 정보를 생성하고 저장한 후 반환합니다.
     *
     * @param productId     채팅을 시작할 상품의 ID
     * @param initiatorUserId 현재 로그인한 ID
     * @return              찾거나 새로 생성된 ChatRoom의 DTO
     */

    // @Transactional: 이 메소드 안의 DB 작업은 모두 한 묶음(트랜잭션)입니다.
    // 하나라도 실패하면 모든 작업을 없던 일로 되돌립니다(롤백).
    @Transactional
    public ChatRoomDTO findOrCreateChatRoom(Long productId, String initiatorUserId) {
        // 1. 필수 정보 조회 (상품, 판매자, 그리고 '채팅을 시작한 사람')
        //findById(상품번호)를 통해 product 테이블에서 상품을 조회
        //DB에서 관련 정보 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("Product not found with id: " + productId));


        MemberEntity seller = product.getMember();
        MemberEntity initiator = memberRepository.findByUserId(initiatorUserId)
                .orElseThrow(() -> new NoSuchElementException("User not found with id: " + initiatorUserId));

        // 2. (순서 변경!) 기존 채팅방이 있는지 *먼저* 확인합니다.
        //기존 채팅방 검색
        // "상품 번호, 참여자 1, 참여자 2" 조건을 만족하는 채팅방을 DB에서 검색합니다.
        // 첫 대화이므로 검색 결과인 existingRooms 리스트는 비어있습니다.
        //findChatRoomByProductAndUsers = 첫 번째 대화인지 구분하는 쿼리문이랑 연결
        //만약 처음이라면 아래 if문을 건너뛰고 새로운 방을 만들어줍니다.
        List<ChatRoom> existingRooms = chatRoomRepository.findChatRoomByProductAndUsers(product, seller, initiator);


        if (!existingRooms.isEmpty()) {
            // 채팅방이 이미 있으면, 역할을 따질 필요 없이 바로 반환합니다. (판매자도 참여 가능)
            ChatRoom existingRoom = existingRooms.get(0);
            return new ChatRoomDTO(existingRoom.getChatId(), existingRoom.getProduct().getPid(),
                    existingRoom.getCreatedAt(), existingRoom.getLastMessageAt(), null);
        }

        // 3. 채팅방이 없을 때만, '스스로에게 채팅을 거는 상황'인지 확인합니다.
        // 판매자는 스스로 '새로운' 채팅방을 만들 수는 없습니다.
        if (seller.getUserNo().equals(initiator.getUserNo())) {
            throw new IllegalArgumentException("Seller cannot start a new chat on their own product.");
        }

        // 4. 여기까지 왔다면, 채팅방이 없고 & 시작한 사람이 구매자라는 의미입니다.
        // 새로운 채팅방을 생성합니다.
        ChatRoom newChatRoom = ChatRoom.builder()
                .product(product)
                .createdAt(LocalDateTime.now())
                .build();
        chatRoomRepository.save(newChatRoom);

        ChatParticipant sellerParticipant = createParticipant(newChatRoom, seller);
        ChatParticipant buyerParticipant = createParticipant(newChatRoom, initiator); // initiator가 곧 buyer
        chatParticipantRepository.saveAll(Arrays.asList(sellerParticipant, buyerParticipant));

        return new ChatRoomDTO(newChatRoom.getChatId(), newChatRoom.getProduct().getPid(),
                newChatRoom.getCreatedAt(), newChatRoom.getLastMessageAt(), null);
    }


    /**
     * ChatParticipant 객체를 생성하는 헬퍼 메소드입니다.
     *
     * @param chatRoom  참여자가 속할 채팅방
     * @param member    참여할 회원
     * @return          생성된 ChatParticipant 객체
     */
    private ChatParticipant createParticipant(ChatRoom chatRoom, MemberEntity member) {
        return ChatParticipant.builder()
                .chatParticipantId(new ChatParticipantId(chatRoom.getChatId(), member.getUserNo()))
                .chatRoom(chatRoom)
                .memberEntity(member)
                .joinedAt(LocalDateTime.now())
                .build();
    }

    /**
     * [대폭 수정된 메소드]
     * 특정 사용자가 참여 중인 모든 채팅방의 상세 정보를 조회하여 ChatRoomListDTO 리스트로 반환합니다.
     * @param userId 현재 로그인한 사용자의 ID
     * @return 채팅 목록에 필요한 모든 정보가 담긴 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<ChatRoomListDTO> findChatRoomDetailsByUserId(String userId) {
        //사용자 ID로 DB에서 MemberEntity(회원 정보)를 조회합니다.
        MemberEntity currentUser = memberRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다: " + userId));

        //조회된 회원 정보(currentUser)를 이용해 해당 회원이 참여하고 있는
        //모든 ChatParticipant(채팅 참여 정보)를 조회합니다.
        List<ChatParticipant> participations = chatParticipantRepository.findByMemberEntity(currentUser);

        //조회된 각 참여 정보를 기반으로 화면에 필요한 DTO를 만듭니다.
        //Java Stream API를 사용하여 코드를 간결하게 만듭니다.
        return participations.stream()
                .filter(participation -> participation.getChatStatus() == ChatStatus.ACTIVE)
                .map(participation -> {
            ChatRoom room = participation.getChatRoom();
            //room에서 상품 아이디를 가져옴
            Product product = room.getProduct();

            // 상대방 정보 찾기
            //채팅방의 모든 참여자 중, 내(currentUser)가 아닌 사람을 찾습니다.
            MemberEntity opponent = room.getParticipants().stream()
                    .map(ChatParticipant::getMemberEntity)
                    //이 member의 회원 번호가 현재 로그인한 나의 회원 번호와 같지 않은(!equals) 것만 통과
                    .filter(member -> !member.getUserNo().equals(currentUser.getUserNo()))

                    .findFirst()
                    .orElse(null);

            // 마지막 메시지 정보 찾기
            //ChatMessageRepository를 이용해 해당 채팅방에서 가장 최근 메시지 1개를 가져옵니다.
            //(ORDER BY created_date DESC LIMIT 1) 조건에 맞는값을 가져옵니다.
            //Optional<ChatMessage> lastMessageOpt = chatMessageRepository.findTopByChatRoomOrderByCreatedDateDesc(room);
            Optional<ChatMessage> lastMessageOpt = chatMessageRepository.findTopByChatRoomOrderByCreatedDateDesc(room);

            //메세지를 가져오는데 없으면 문자 반환
            //String lastMessageContent = lastMessageOpt.map(ChatMessage::getContent).orElse("아직 대화 내용이 없습니다.");
            String lastMessageContent;
            //마지막 메시지 시간 가져오기
            //Optional.empty 이면 -> 채팅방 생성 시간(room.getCreatedAt())을 대신 반환
            //LocalDateTime lastMessageAt = lastMessageOpt.map(ChatMessage::getCreatedDate).orElse(room.getCreatedAt());
            LocalDateTime lastMessageAt;
            if (lastMessageOpt.isPresent()) {
                ChatMessage lastMessage = lastMessageOpt.get();
                // 헬퍼 메소드를 호출하여 미리보기 텍스트를 생성합니다.
                lastMessageContent = formatLastMessagePreview(lastMessage, currentUser);
                lastMessageAt = lastMessage.getCreatedDate();
            } else {
                lastMessageContent = "아직 대화 내용이 없습니다.";
                lastMessageAt = room.getCreatedAt();
            }

            // 안 읽은 메시지 존재 여부 확인
            boolean hasUnread = chatMessageRepository.existsByChatRoom_ChatIdAndSender_UserIdNotAndIsReadFalse(room.getChatId(), userId);

            // DTO 꾸러미에 정보 채우기
            //위에서 수집한 모든 정보를 ChatRoomListDTO에 담아 최종적으로 반환합니다.
            return ChatRoomListDTO.builder()
                    .chatId(room.getChatId())
                    .productId(product.getPid())
                    .productTitle(product.getName())
                    .productPrice(product.getPrice())
                    .productImageUrl(product.getImageUrl())
                    .opponentNickname(opponent != null ? opponent.getNickname() : "(알 수 없음)")
                    .lastMessageContent(lastMessageContent)
                    .lastMessageAt(lastMessageAt)
                    .hasUnreadMessages(hasUnread) // DTO에 값 설정
                    .build();
        }).collect(Collectors.toList());
    }



    /**
     * 마지막 메시지를 채팅 목록 미리보기용 텍스트로 변환합니다.
     * @param lastMessage 마지막 ChatMessage 객체
     * @param currentUser 현재 로그인한 사용자
     * @return 미리보기용으로 가공된 문장
     */
    private String formatLastMessagePreview(ChatMessage lastMessage, MemberEntity currentUser) {
        MessageType type = lastMessage.getMessageType();

        // 현재 사용자가 판매자인지 구매자인지 확인하기 위해 거래 정보를 가져옵니다.
        // 일반 메시지는 거래 정보가 필요 없으므로 null 체크를 합니다.
        Transaction transaction = lastMessage.getChatRoom().getProduct().getTransactions().stream()
                .findFirst() // 해당 상품과 연결된 거래를 찾습니다.
                .orElse(null);

        String currentUserId = currentUser.getUserId();

        switch (type) {
            case TEXT:
                return lastMessage.getContent();
            case IMAGE:
                return "写真を送りました。";

            // --- システム 메시지 타입별로 분기 ---
            case TRANSACTION_REQUEST:
                return "購入リクエストが届きました。";

            case TRANSACTION_TYPE_SELECT:
                // 요청을 보낸 사람은 판매자이므로, 판매자 시점에서는 구매자가 선택중이라는 메시지를 보여줍니다.
                if (transaction != null && currentUserId.equals(transaction.getProduct().getMember().getUserId())) {
                    return "購入者が取引方法を選択しています。";
                }
                return "取引方法を選択してください。";

            case PAYMENT_METHOD_SELECT:
                if (transaction != null && currentUserId.equals(transaction.getProduct().getMember().getUserId())) {
                    return "購入者がお支払い方法を選択しています。";
                }
                return "お支払い方法を選択してください。";

            case SHIPPING_INFO_REQUEST:
                return "送り状情報を入力してください。";

            case PURCHASE_CONFIRM_REQUEST:
                return "購入確定を待っています。";

            case REVIEW_REQUEST:
                return "取引レビューを残してください。";

            default:
                // 혹시 모를 다른 타입에 대비
                return "システムメッセージが届きました。";
        }
    }


    /**
     * 사용자가 조건에 따라 채팅방을 나갈 수 있게 합니다.
     * @param chatId 나가려는 채팅방의 ID
     * @param userId 나가는 사용자의 ID
     */
    @Transactional
    //
    public void leaveChatRoom(Long chatId, String userId) {
        // 1. 채팅방과 연결된 상품 정보를 조회합니다.
        ChatRoom room = chatRoomRepository.findById(chatId)
                .orElseThrow(() -> new NoSuchElementException("Chat room not found with id: " + chatId));
        Product product = room.getProduct();

        // 2. 조건 확인: 상품이 '예약 중(RESERVED)' 상태이면 퇴장을 거부합니다.
        if (product.getStatus() == ProductStatus.RESERVED) {
            // 사용자에게 보여줄 에러 메시지는 일본어로 설정합니다.
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "予約中の商品のため、チャットルームから退出できません。");
        }

        // 3. 해당 채팅방에서 현재 사용자의 참여 정보를 찾습니다.
        ChatParticipant participant = room.getParticipants().stream()
                .filter(p -> p.getMemberEntity().getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Participant not found for user " + userId + " in chat " + chatId));

        // 4. 참여 상태를 'EXITED'로 변경합니다.
        participant.setChatStatus(ChatStatus.EXITED);

        // @Transactional 어노테이션에 의해 메소드가 종료될 때 변경 사항이 DB에 자동으로 저장됩니다.
    }

}



