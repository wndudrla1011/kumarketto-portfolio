package org.dsa11.team1.kumarketto.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsa11.team1.kumarketto.domain.dto.ChatMessageDTO;
import org.dsa11.team1.kumarketto.domain.dto.ChatMessageSendDTO;
import org.dsa11.team1.kumarketto.domain.entity.ChatMessage;
import org.dsa11.team1.kumarketto.domain.entity.ChatParticipant;
import org.dsa11.team1.kumarketto.domain.entity.ChatRoom;
import org.dsa11.team1.kumarketto.domain.entity.MemberEntity;
import org.dsa11.team1.kumarketto.domain.enums.ChatStatus;
import org.dsa11.team1.kumarketto.repository.ChatMessageRepository;
import org.dsa11.team1.kumarketto.repository.ChatParticipantRepository;
import org.dsa11.team1.kumarketto.repository.ChatRoomRepository;
import org.dsa11.team1.kumarketto.repository.MemberRepository;
import org.dsa11.team1.kumarketto.service.ChatMessageService;
import org.dsa11.team1.kumarketto.service.ChatRoomService;
import org.dsa11.team1.kumarketto.service.MemberService;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandler extends TextWebSocketHandler {

    //private final ObjectMapper objectMapper = new ObjectMapper();
    private final ObjectMapper objectMapper;
    private final ChatMessageService chatMessageService;
    private final ChatRoomService chatRoomService;
    //private final ChatParticipantService chatParticipantService;
    private final MemberService memberService;
    private final MemberRepository memberRepository;


    // userSessions: 현재 서버에 접속한 모든 사용자의 웹소켓 세션(통신 터널)을
    //               저장하는 동시성 지원 맵(ConcurrentHashMap)입니다.
    //               Key: 유저 아이디(String), Value: 웹소켓 세션 객체(WebSocketSession)
    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();

//    private final Map<String, Set<String>> participants = new ConcurrentHashMap<>();

    private  final Map<String, List<ChatMessage>> pendingMessages = new ConcurrentHashMap<>();

//    private final Map<String, Instant> lastMessageAt = new ConcurrentHashMap<>();

    // 'afterConnectionEstablished'는 클라이언트와 웹소켓 연결이 성공적으로 맺어지면 자동 실행됩니다.
    // 'session' 파라미터는 방금 연결된 클라이언트와의 1:1 통신 파이프입니다.
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // Interceptor가 연결 과정에서 붙여준 'userId' 를 꺼냅니다.
        //**HttpSession**이라는 눈에 보이지 않는 자유이용권을 발급합니다. 여기에는 userId 같은 정보가 기록됩니다.
        String userId = (String) session.getAttributes().get("userId");
        if (userId == null) {
            log.warn("Unauthenticated session tried to connect");
            // 이름표가 없으면 강제로 연결을 끊습니다.
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        // 'userSessions'라는 온라인 사용자 목록(Map)에 '아이디'와 그의 통신 파이프(session)를 기록합니다.
        // userSessions 맵에는 이제 {"유저아이디": session객체} 와 같은 데이터가 저장됩니다.
        //"이 아이디(userId)의 사용자는 이 통신 터널(session)을 사용합니다"라고 기록합니다.
        userSessions.put(userId, session);

        log.info("Connected: userId={} sessionId={}", userId, session.getId());

        deliverPendingMessagesToUser(userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        //원본 데이터(JSON 문자열)를 꺼내고, ChatMessageDTO 자바 객체로 변환합니다.
        // 1. 수신한 텍스트 메시지에서 순수 데이터(JSON 문자열)를 꺼냅니다.
        String payload = message.getPayload();

        // 2. ObjectMapper를 이용해 JSON 문자열을 ChatMessageDTO 자바 객체로 변환합니다.
        //    (예: '{"chatId":1, "content":"안녕"}' -> ChatMessageDTO 객체)
        ChatMessageDTO chatMessageDTO = objectMapper.readValue(payload, ChatMessageDTO.class);


        //메시지를 보낸 사람의 ID를 안전하게 세션에서 가져옵니다.
        // 3. 메시지를 보낸 사람의 ID는 세션에 저장된 '이름표'에서 가져와 설정합니다.
        //    (보안상 클라이언트가 보내준 senderId를 믿지 않고, 서버가 관리하는 ID를 사용)
        String senderId = (String) session.getAttributes().get("userId");

        //메시지를 DB에 저장하고, 저장된 완전한 ChatMessage 객체를 돌려받습니다.
        chatMessageDTO.setSenderId(senderId);

        // 4. 메시지 생성 시간을 서버 시간 기준으로 설정합니다.
        chatMessageDTO.setCreatedDate(LocalDateTime.now());



        MemberEntity memberEntity = memberRepository.findByUserId(senderId).orElseThrow(() -> new IllegalStateException("No user found: " + senderId));
        if(memberEntity.getRole().equals("ADMIN")) {
            broadcastNotice(chatMessageDTO);
            return;
        }

       // ChatMessage chatMessage = chatMessageService.saveMessage(chatMessageDTO);

        ChatMessage chatMessage;

        // DTO에 imageUrl 필드가 있는지 확인하여 분기
        if (chatMessageDTO.getImageUrl() != null && !chatMessageDTO.getImageUrl().isEmpty()) {
            // 이미지 URL이 있으면 이미지 저장 서비스 호출
            chatMessage = chatMessageService.saveImageMessage(chatMessageDTO);
        } else {
            // 이미지 URL이 없으면 기존 텍스트 저장 서비스 호출
            chatMessage = chatMessageService.saveMessage(chatMessageDTO);
        }

        //  "chat_id 채팅방의 활성(ACTIVE) 참여자 목록을 DB에서 조회해줘"라고 요청합니다.
        // chatParticipantSet 변수에는 user1아이디와 user2아이디의 정보가 담긴 Set이 할당됩니다.
        Set<ChatParticipant> chatParticipantSet = chatMessageService.getActiveParticipants(chatMessageDTO.getChatId());

        // 기존 for문을 아래 코드로 교체해주세요.
        System.out.println("\n--- [최종 송수신 디버그] ---");
        System.out.println("보낸 사람: '" + senderId + "' (글자 수: " + senderId.length() + ")");
        System.out.println("현재 전체 온라인 사용자 목록 (userSessions):");
        for (String key : userSessions.keySet()) {
            System.out.println("  - Key: '" + key + "' (글자 수: " + key.length() + ")");
        }
        System.out.println("--------------------------");

        // 'for' 반복문: 참여자 목록에서 한 명씩 꺼내 중괄호 안의 코드를 실행합니다.
        // for-each 루프: "chatParticipantSet 그룹에서 멤버를 한 명씩 꺼내
//                chatParticipant 라는 임시 변수에 담아서,
//                아래 중괄호 {} 안의 코드를 멤버 수만큼 반복 실행해줘."
        for (ChatParticipant chatParticipant : chatParticipantSet) {
            String recipientId = chatParticipant.getMemberEntity().getUserId();
            System.out.println("\n> 참여자 확인 중... DB에서 가져온 ID: '" + recipientId + "' (글자 수: " + recipientId.length() + ")");

            // 만약 참여자가 메시지를 보낸 자신이라면, 건너뜁니다(continue).
            if (recipientId.equals(senderId)) {
                System.out.println("  -> 보낸 사람이므로 건너뜁니다.");
                continue;
            }

            System.out.println("  -> 수신자 발견! 세션 찾기 시도...");

            // 맵에 키가 실제로 존재하는지 containsKey로 다시 한번 확인합니다.
            boolean isKeyPresent = userSessions.containsKey(recipientId);
            System.out.println("  -> '온라인 사용자 목록'에 수신자('" + recipientId + "')가 있습니까? : " + isKeyPresent);

            if (isKeyPresent) {
                //  온라인 사용자 목록에서 상대방의 통신 파이프를 찾습니다.

                WebSocketSession recipientSession = userSessions.get(recipientId);
                System.out.println("  -> 세션을 성공적으로 찾았습니다!");
                System.out.println("  -> 세션이 열려 있습니까? : " + (recipientSession != null && recipientSession.isOpen()));

                try {
                    // 전송용 DTO(ChatMessageSendDTO)로 변환합니다.
                    ChatMessageSendDTO dtoToSend = ChatMessageSendDTO.fromEntity(chatMessage);
                    String messageToSend = objectMapper.writeValueAsString(dtoToSend);
                    recipientSession.sendMessage(new TextMessage(messageToSend));
                    System.out.println("  -> 메시지 전송 성공!");
                } catch (Exception e) {
                    System.out.println("  -> !!! 메시지 전송 중 예외 발생: " + e.getMessage());
                }
            } else {
                System.out.println("  -> !!! [치명적] '온라인 사용자 목록'에 수신자 키가 없습니다.");
                System.out.println("  -> 보류 메시지함에 저장합니다.");
                pendingMessages.computeIfAbsent(recipientId, k -> new ArrayList<>()).add(chatMessage);
            }
        }
        System.out.println("--- [디버그 종료] ---\n");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = (String) session.getAttributes().get("userId");
        if(userId != null) {
            userSessions.remove(userId);
            log.info("Disconnected: userId={} sessionId={}", userId, session.getId());
        }
    }

    private void deliverPendingMessagesToUser(String userId) {
        WebSocketSession session = userSessions.get(userId);
        if (session == null || !session.isOpen()) return;

        List<ChatMessage> pending = pendingMessages.remove(userId);
        if(pending != null) {
            for(ChatMessage msg : pending) {
                try {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
                } catch (Exception e) {
                    log.error("Failed to deliver pending message to user {}", userId, e);
                    pendingMessages.computeIfAbsent(userId, k -> new ArrayList<>()).add(msg);
                }
            }
        }
    }

    public void broadcastNotice(ChatMessageDTO chatMessageDTO) {
        try {
//            chatMessageDTO.setCreatedDate(LocalDateTime.now());
            ChatMessage chatMessage = chatMessageService.saveMessage(chatMessageDTO);

            String msgJson = objectMapper.writeValueAsString(chatMessage);

            List<String> allUserIds = memberRepository.findAllUserIds();

            int sentCount = 0;

            for(String userId : allUserIds) {
                WebSocketSession session = userSessions.get(userId);

                if(session != null && session.isOpen()) {
                    try{
                        session.sendMessage(new TextMessage(msgJson));
                        sentCount++;
                    } catch (Exception e) {
                        log.error("Failed to send notice to user {}", userId, e);
                        pendingMessages.computeIfAbsent(userId, k -> new ArrayList<>()).add(chatMessage);
                    }
                } else {
                    pendingMessages.computeIfAbsent(userId, k -> new ArrayList<>()).add(chatMessage);
                }
            }
            log.info("Broadcast notice sent to {} users", sentCount);
        } catch (Exception e) {
            log.error("Failed to broadcast notice", e);
        }
    }

    /**
     * 특정 사용자에게 메시지 읽음 확인 알림을 보냅니다.
     * @param userId 알림을 받을 사용자 ID (메시지를 보냈던 사람)
     * @param chatId 읽음 처리된 채팅방 ID
     */
    public void sendReadConfirmation(String userId, Long chatId) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                // 프론트엔드와 약속된 형식의 시스템 메시지를 만듭니다.
                Map<String, Object> payload = Map.of(
                        "type", "MESSAGES_READ",
                        "chatId", chatId
                );
                String message = objectMapper.writeValueAsString(payload);
                session.sendMessage(new TextMessage(message));
                log.info("Sent read confirmation to user {} for chat {}", userId, chatId);
            } catch (Exception e) {
                log.error("Failed to send read confirmation", e);
            }
        }
    }


    // [수정] 시스템 메시지를 전송하는 메소드 추가
    public void sendSystemMessage(ChatMessageDTO chatMessageDTO) {
        try {
            // 1. 시스템 메시지를 먼저 DB에 저장하여 기록을 남깁니다.
            chatMessageDTO.setCreatedDate(LocalDateTime.now());
            ChatMessage savedMessage = chatMessageService.saveMessage(chatMessageDTO);

            // 2. 해당 채팅방에 참여중인 모든 활성 사용자에게 메시지를 전송합니다.
            Set<ChatParticipant> participants = chatMessageService.getActiveParticipants(chatMessageDTO.getChatId());

            ChatMessageSendDTO dtoToSend = ChatMessageSendDTO.fromEntity(savedMessage);
            String messageToSend = objectMapper.writeValueAsString(dtoToSend);

            for (ChatParticipant participant : participants) {
                String recipientId = participant.getMemberEntity().getUserId();
                WebSocketSession recipientSession = userSessions.get(recipientId);

                // 사용자가 현재 접속중(온라인)이라면 메시지를 보냅니다.
                if (recipientSession != null && recipientSession.isOpen()) {
                    recipientSession.sendMessage(new TextMessage(messageToSend));
                    log.info("System message sent to user {}", recipientId);
                }
            }
        } catch (Exception e) {
            log.error("Failed to send system message", e);
        }
    }






    // 사용자가 접속할 때 userSessionMap에 추가하는 로직이 필요합니다. (onOpen)
// 사용자가 접속을 끊을 때 userSessionMap에서 제거하는 로직도 필요합니다. (onClose)

    public void sendTargetedSystemMessage(ChatMessageDTO message, String targetUserId) {
        // 1. 시스템 메시지도 DB에 저장해야 나중에 다시 접속했을 때 볼 수 있습니다.
        message.setCreatedDate(LocalDateTime.now());
        ChatMessage savedMessage = chatMessageService.saveMessage(message);

        //  userSessionMap 대신 기존에 사용하던 userSessions 맵을 사용합니다.
        WebSocketSession targetSession = userSessions.get(targetUserId);

        // 3. 상대방이 온라인 상태일 때
        if (targetSession != null && targetSession.isOpen()) {
            try {
                // 프론트엔드로 보낼 DTO로 변환
                ChatMessageSendDTO dtoToSend = ChatMessageSendDTO.fromEntity(savedMessage);
                String messageToSend = objectMapper.writeValueAsString(dtoToSend);
                targetSession.sendMessage(new TextMessage(messageToSend));
                log.info("Targeted system message sent to user {}", targetUserId);
            } catch (IOException e) {
                log.error("타겟 메시지 전송 실패", e);
            }
        }
        // 4.상대방이 오프라인일 때 -> 보류 메시지함에 추가
        else {
            log.info("Target user {} is not online. Message will be stored in pending messages.", targetUserId);
            pendingMessages.computeIfAbsent(targetUserId, k -> new ArrayList<>()).add(savedMessage);
        }
    }


}

