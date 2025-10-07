package org.dsa11.team1.kumarketto.controller;

import lombok.RequiredArgsConstructor;
import org.dsa11.team1.kumarketto.domain.dto.ChatMessageSendDTO;
import org.dsa11.team1.kumarketto.domain.dto.ChatRoomDTO;
import org.dsa11.team1.kumarketto.domain.dto.ChatRoomListDTO;
import org.dsa11.team1.kumarketto.domain.entity.ChatParticipant;
import org.dsa11.team1.kumarketto.security.AuthenticatedUser;
import org.dsa11.team1.kumarketto.service.ChatMessageService;
import org.dsa11.team1.kumarketto.service.ChatRoomService;
import org.dsa11.team1.kumarketto.websocket.WebSocketHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 채팅 관련 데이터(JSON)를 제공하는 API 전용 컨트롤러입니다.
 * @RestController 어노테이션을 사용합니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat") // 이 컨트롤러의 모든 주소는 /api/chat 으로 시작합니다.
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final WebSocketHandler webSocketHandler;
    /**
     *
     * 반환 타입을 List<ChatRoomListDTO>로 변경하고,
     * 새로운 서비스 메소드인 findChatRoomDetailsByUserId를 호출합니다.
     */
    @GetMapping("/my-rooms")
    public ResponseEntity<List<ChatRoomListDTO>> getMyChatRooms(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        String currentUserId = authenticatedUser.getUsername();
        // 서비스 메소드 호출 변경
        List<ChatRoomListDTO> chatRooms = chatRoomService.findChatRoomDetailsByUserId(currentUserId);
        return ResponseEntity.ok(chatRooms);
    }


    // 상품 페이지에서 '채팅하기'를 눌렀을 때 새로운 채팅방을 만드는 아래 메소드들은 그대로 필요합니다.
    @PostMapping("/room/buyer")
    public ResponseEntity<ChatRoomDTO> findOrCreateChatRoomForBuyer(@RequestParam("productId") Long productId,
                                                                    @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {


        String buyerUserId = authenticatedUser.getUsername();
        ChatRoomDTO chatRoomDTO = chatRoomService.findOrCreateChatRoom(productId, buyerUserId);
        return ResponseEntity.ok(chatRoomDTO);
    }

    @PostMapping("/room/seller")
    public ResponseEntity<ChatRoomDTO> findChatRoomForSeller(@RequestParam("productId") Long productId,
                                                             @RequestParam("buyerId") String buyerId) {
        ChatRoomDTO chatRoomDTO = chatRoomService.findOrCreateChatRoom(productId, buyerId);
        return ResponseEntity.ok(chatRoomDTO);
    }


    /**
     * 특정 채팅방의 이전 메시지 기록을 조회하는 API
     * @param chatId 메시지를 조회할 채팅방 ID
     * @param authenticatedUser 현재 로그인한 사용자 정보 (Spring Security가 주입)
     * @return 메시지 목록 (JSON 배열)
     */
    @GetMapping("/{chatId}/messages")
    public ResponseEntity<List<ChatMessageSendDTO>> getChatMessages(
            @PathVariable("chatId") Long chatId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser // <-- 이 부분 추가!
    ) {
        // 현재 사용자의 ID를 서비스 계층으로 전달합니다.
        String currentUserId = authenticatedUser.getUsername();
        List<ChatMessageSendDTO> messages = chatMessageService.getMessagesByChatId(chatId, currentUserId); //
        return ResponseEntity.ok(messages);
    }

    /**
     * 특정 채팅방의 메시지를 모두 읽음 처리하는 API
     * @param chatId 읽음 처리할 채팅방 ID
     * @param userDetails 현재 로그인한 사용자 정보
     */
    @PostMapping("/{chatId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable("chatId") Long chatId,
                                           @AuthenticationPrincipal UserDetails userDetails) { // 1. 파라미터를 UserDetails 타입으로 받습니다.

        // 2. AuthenticatedUser 타입으로 강제 형변환(casting)합니다.
        AuthenticatedUser authenticatedUser = (AuthenticatedUser) userDetails;

        // 3. 이제 getUserNo()와 getUsername()을 모두 정상적으로 호출할 수 있습니다.
        // 로그인한 사용자의 고유 번호(PK)
        Long currentUserNo = authenticatedUser.getUserNo();
        String currentUserId = authenticatedUser.getUsername();


        chatMessageService.markMessagesAsRead(chatId, currentUserNo);

        // WebSocket 알림 로직은 그대로 유지됩니다.
        //String currentUserId = authenticatedUser.getUsername();
        Set<ChatParticipant> participants = chatMessageService.getActiveParticipants(chatId);

        participants.stream()
                .map(p -> p.getMemberEntity().getUserId())
                .filter(userId -> !userId.equals(currentUserId))
                .findFirst()
                .ifPresent(opponentId -> {
                    webSocketHandler.sendReadConfirmation(opponentId, chatId);
                });

        return ResponseEntity.ok().build();
    }

    /**
     * 사용자가 특정 채팅방을 나가는 요청을 처리하는 엔드포인트입니다.
     *
     * @param chatId 나가려는 채팅방의 ID
     * @param authenticatedUser 현재 로그인된 사용자 정보
     * @return 성공 또는 실패에 대한 응답
     */
    @PostMapping("/{chatId}/leave")
    // [수정] 중복 선언되었던 메소드를 하나로 합쳤습니다.
    public ResponseEntity<?> leaveChatRoom(@PathVariable("chatId") Long chatId,
                                           @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        try {
            String currentUserId = authenticatedUser.getUsername();
            chatRoomService.leaveChatRoom(chatId, currentUserId);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) {
            // 서비스 계층에서 발생한 예외의 상태 코드와 메시지를 그대로 클라이언트에게 전달합니다.
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason()));
        } catch (Exception e) {
            // 그 외 예상치 못한 에러를 처리합니다.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "予期せぬエラーが発生しました。"));
        }
    }
}