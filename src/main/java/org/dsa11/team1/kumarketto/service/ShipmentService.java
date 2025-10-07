package org.dsa11.team1.kumarketto.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.dsa11.team1.kumarketto.domain.dto.ChatMessageDTO;
import org.dsa11.team1.kumarketto.domain.dto.ChatRoomDTO;
import org.dsa11.team1.kumarketto.domain.dto.ShipmentRequestDTO;
import org.dsa11.team1.kumarketto.domain.dto.ShipmentResponseDTO;
import org.dsa11.team1.kumarketto.domain.entity.MemberEntity;
import org.dsa11.team1.kumarketto.domain.entity.Shipment;
import org.dsa11.team1.kumarketto.domain.entity.Transaction;
import org.dsa11.team1.kumarketto.domain.enums.DeliveryService;
import org.dsa11.team1.kumarketto.domain.enums.PaymentStatus;
import org.dsa11.team1.kumarketto.domain.enums.MessageType;
import org.dsa11.team1.kumarketto.domain.enums.TransactionStatus;
import org.dsa11.team1.kumarketto.repository.ShipmentRepository;
import org.dsa11.team1.kumarketto.repository.TransactionRepository;
import org.dsa11.team1.kumarketto.websocket.WebSocketHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final TransactionRepository transactionRepository;
    private final ChatRoomService chatRoomService;
    private final WebSocketHandler webSocketHandler;

    /**
     * 배송 정보 등록
     * @param transactionId     거래 ID
     * @param sellerUserNo      판매자 ID
     * @param requestDTO        배송 정보 요청 DTO
     * @return  ShipmentResponseDTO
     */
    public ShipmentResponseDTO createShipment(Long transactionId, Long sellerUserNo, ShipmentRequestDTO requestDTO) {

        // 거래 정보 조회
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("거래를 찾을 수 없습니다."));

        /* Validation */
        // 단일 사용자 테스트이므로 임시 비활성화
//        if (!transaction.getProduct().getMember().getUserNo().equals(sellerUserNo)) {
//            throw new SecurityException("배송 정보를 등록할 권한이 없습니다.");
//        }

        if (transaction.getStatus() != TransactionStatus.PAID) {
            throw new IllegalStateException("결제가 완료된 거래만 배송 정보 등록 가능");
        }

        if (transaction.getDeliveryService() != DeliveryService.DELIVERY_SERVICE) {
            throw new IllegalStateException("택배 거래 상품만 배송 정보 등록 가능");
        }

        if (transaction.getShipment() != null) {
            throw new IllegalStateException("이미 배송 정보가 등록되었습니다.");
        }

        // Shipment 생성
        Shipment shipment = Shipment.builder()
                .transaction(transaction)
                .courier(requestDTO.getCourier())
                .trackingNumber(requestDTO.getTrackingNumber())
                .shippedAt(LocalDateTime.now())
                .build();

        // 연관 관계 설정 및 저장
        transaction.bindShipment(shipment);

        //운송장 등록 완료 후, 구매자에게 '구매 확정' 메시지 전송
        sendPurchaseConfirmationToBuyer(transaction);

        return new ShipmentResponseDTO(shipment);

    }

    /**
     * 구매자에게 구매 확정 요청 메시지를 전송하는 메소드
     * @param transaction 운송장 등록이 완료된 거래 객체
     */
    private void sendPurchaseConfirmationToBuyer(Transaction transaction) {
        MemberEntity buyer = transaction.getMember();
        MemberEntity seller = transaction.getProduct().getMember();
        Long productId = transaction.getProduct().getPid();

        ChatRoomDTO chatRoomDTO = chatRoomService.findOrCreateChatRoom(productId, buyer.getUserId());
        String messageContent = String.format("{\"transactionId\": %d}", transaction.getId());

        ChatMessageDTO systemMessage = ChatMessageDTO.builder()
                .chatId(chatRoomDTO.getChatId())
                .senderId(seller.getUserId()) // 판매자가 보낸 것으로 설정
                .content(messageContent)
                .messageType(MessageType.PURCHASE_CONFIRM_REQUEST)
                .build();

        // 방송 대신 '구매자'에게만 타겟팅하여 메시지 전송
        // webSocketHandler.sendSystemMessage(systemMessage); // 기존 방송 메소드
        webSocketHandler.sendTargetedSystemMessage(systemMessage, buyer.getUserId()); // 수정된 타겟팅 메소드

    }

}
