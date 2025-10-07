package org.dsa11.team1.kumarketto.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsa11.team1.kumarketto.domain.dto.ChatMessageDTO;
import org.dsa11.team1.kumarketto.domain.dto.ChatMessageSendDTO;
import org.dsa11.team1.kumarketto.domain.entity.*;
import org.dsa11.team1.kumarketto.domain.enums.ChatStatus;
import org.dsa11.team1.kumarketto.domain.enums.MessageType;
import org.dsa11.team1.kumarketto.domain.enums.TransactionStatus;
import org.dsa11.team1.kumarketto.repository.ChatMessageRepository;
import org.dsa11.team1.kumarketto.repository.ChatRoomRepository;
import org.dsa11.team1.kumarketto.repository.MemberRepository;
import org.dsa11.team1.kumarketto.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MemberRepository memberRepository;
    private final ObjectMapper objectMapper;
    private final TransactionRepository transactionRepository;

    public ChatMessage saveMessage(ChatMessageDTO chatMessageDTO) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatMessageDTO.getChatId())
                .orElseThrow(() -> new IllegalArgumentException("ChatRoom not found: " + chatMessageDTO.getChatId()));
        MemberEntity memberEntity = memberRepository.findByUserId(chatMessageDTO.getSenderId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + chatMessageDTO.getSenderId()));

        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(memberEntity)
                .content(chatMessageDTO.getContent())
                .imageUrl(chatMessageDTO.getImageUrl())
                .messageType(chatMessageDTO.getMessageType())
                .createdDate(chatMessageDTO.getCreatedDate())
                .build();

        chatRoom.setLastMessageAt(chatMessage.getCreatedDate());
        return chatMessageRepository.save(chatMessage);
    }

    public Set<ChatParticipant> getActiveParticipants(Long chatId) {
        ChatRoom chatRoom = chatRoomRepository.findByIdWithParticipantsAndMembers(chatId)
                .orElseThrow(() -> new NoSuchElementException("ChatRoom not found" + chatId));
        return chatRoom.getParticipants().stream()
                .filter(participant -> participant.getChatStatus() == ChatStatus.ACTIVE)
                .collect(Collectors.toSet());
    }

    // ===================================================================================
    //
    // ===================================================================================
    @Transactional(readOnly = true)
    public List<ChatMessageSendDTO> getMessagesByChatId(Long chatId, String currentUserId) {
        List<ChatMessage> allMessages = chatMessageRepository.findByChatRoom_ChatIdOrderByCreatedDateAsc(chatId);
        List<ChatMessageSendDTO> filteredMessages = new ArrayList<>();

        // 채팅방에 연결된 거래 정보를 미리 한 번만 조회 (성능 개선)
        Transaction mainTransaction = transactionRepository.findTransactionByChatId(chatId).orElse(null);

        for (ChatMessage message : allMessages) {
            ChatMessageSendDTO dtoToShow = null;
            MessageType type = message.getMessageType();

            // 1. 일반 메시지(TEXT, IMAGE)는 항상 포함
            if (type == MessageType.TEXT || type == MessageType.IMAGE) {
                dtoToShow = ChatMessageSendDTO.fromEntity(message);
            }
            // 2. 시스템 메시지는 최신 거래 상태를 확인하여 동적으로 변환
            else if (mainTransaction != null) {
                TransactionStatus currentStatus = mainTransaction.getStatus();
                String buyerId = mainTransaction.getMember().getUserId();
                String sellerId = mainTransaction.getProduct().getMember().getUserId();

                try {
                    switch (type) {
                        case TRANSACTION_REQUEST:
                            // PENDING 상태일 때만 판매자에게 버튼을 보여줌
                            if (currentStatus == TransactionStatus.PENDING && currentUserId.equals(sellerId)) {
                                dtoToShow = ChatMessageSendDTO.fromEntity(message);
                            }
                            // 상태가 바뀌었다면, 버튼 대신 '상태 텍스트'로 변환하여 보여줌
                            else if (currentStatus == TransactionStatus.APPROVED) {
                                dtoToShow = createSystemTextView("판매자가 거래를 승인했습니다.", message.getCreatedDate().plusNanos(1));
                            } else if (currentStatus == TransactionStatus.REJECTED) {
                                dtoToShow = createSystemTextView("판매자가 거래를 거절했습니다.", message.getCreatedDate().plusNanos(1));
                            }
                            break;

                        case TRANSACTION_TYPE_SELECT:
                        case PAYMENT_METHOD_SELECT:
                            // APPROVED 상태일 때만 구매자에게 버튼을 보여줌
                            if (currentStatus == TransactionStatus.APPROVED && currentUserId.equals(buyerId)) {
                                dtoToShow = ChatMessageSendDTO.fromEntity(message);
                            }
                            break;

                        case SHIPPING_INFO_REQUEST:
                            if (currentStatus == TransactionStatus.PAID && currentUserId.equals(sellerId)) {
                                dtoToShow = ChatMessageSendDTO.fromEntity(message);
                            }
                            break;

                        case CASH_PAYMENT_SELECTED:
                            if (currentStatus == TransactionStatus.PAID && currentUserId.equals(sellerId)) {
                                dtoToShow = ChatMessageSendDTO.fromEntity(message);
                            }
                            break;

                        case ITEM_RECEIVED_CHECK:
                            if(currentStatus == TransactionStatus.PAID && currentUserId.equals(buyerId)){
                                dtoToShow = ChatMessageSendDTO.fromEntity(message);
                            }
                            break;

                        case PURCHASE_CONFIRM_REQUEST:
                            if (currentStatus == TransactionStatus.PAID && currentUserId.equals(buyerId)) {
                                dtoToShow = ChatMessageSendDTO.fromEntity(message);
                            }
                            break;

                        case REVIEW_REQUEST:
                            // CONFIRMED 상태일 때만 리뷰 요청 버튼을 보여줌
                            if (currentStatus == TransactionStatus.CONFIRMED) {
                                dtoToShow = ChatMessageSendDTO.fromEntity(message);
                            }
                            break;
                    }
                } catch (Exception e) {
                    log.error("Error processing system message: ", e);
                }
            }
            if (dtoToShow != null) {
                filteredMessages.add(dtoToShow);
            }
        }

        // 최종 상태를 알려주는 텍스트를 추가
        if (mainTransaction != null) {
            if (mainTransaction.getStatus() == TransactionStatus.PAID && mainTransaction.getPayment() != null) {
                filteredMessages.add(createSystemTextView("결제가 완료되었습니다.", mainTransaction.getPayment().getPaymentTime()));
            } else if (mainTransaction.getStatus() == TransactionStatus.CONFIRMED) {
                filteredMessages.add(createSystemTextView("거래가 성공적으로 완료되었습니다.", mainTransaction.getConfirmTime()));
            }
        }

        // 시간 순으로 최종 정렬 (nullsLast는 혹시 모를 timestamp null 값에 대비)
        filteredMessages.sort(Comparator.comparing(ChatMessageSendDTO::getCreatedDate, Comparator.nullsLast(Comparator.naturalOrder())));

        return filteredMessages;
    }

    /**
     * 시스템 텍스트 메시지 DTO를 생성하는 헬퍼 메소드
     */
    private ChatMessageSendDTO createSystemTextView(String text, LocalDateTime timestamp) {
        if (timestamp == null) {
            timestamp = LocalDateTime.now(); // timestamp가 null일 경우 현재 시간으로 설정
        }
        return ChatMessageSendDTO.builder()
                .senderId("system") // 시스템 발신자
                .messageType(MessageType.TEXT) // 일반 텍스트 메시지로 표시
                .content(text)
                .createdDate(timestamp)
                .isRead(true) // 항상 읽음 처리
                .build();
    }

    @Transactional
    public void markMessagesAsRead(Long chatId, Long recipientUserNo) {
        chatMessageRepository.markAllAsReadByRecipient(chatId, recipientUserNo);
    }

    @Transactional
    public ChatMessage saveImageMessage(ChatMessageDTO chatMessageDTO) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatMessageDTO.getChatId())
                .orElseThrow(() -> new IllegalArgumentException("ChatRoom not found: " + chatMessageDTO.getChatId()));
        MemberEntity sender = memberRepository.findByUserId(chatMessageDTO.getSenderId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + chatMessageDTO.getSenderId()));

        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .content("사진")
                .imageUrl(chatMessageDTO.getImageUrl())
                .messageType(MessageType.IMAGE)
                .createdDate(chatMessageDTO.getCreatedDate())
                .isRead(false)
                .build();

        chatRoom.setLastMessageAt(chatMessage.getCreatedDate());
        return chatMessageRepository.save(chatMessage);
    }
}