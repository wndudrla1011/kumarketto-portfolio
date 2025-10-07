package org.dsa11.team1.kumarketto.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.dsa11.team1.kumarketto.domain.dto.ChatMessageDTO;
import org.dsa11.team1.kumarketto.domain.dto.ChatRoomDTO;
import org.dsa11.team1.kumarketto.domain.dto.PaymentIntentResponseDTO;
import org.dsa11.team1.kumarketto.domain.entity.MemberEntity;
import org.dsa11.team1.kumarketto.domain.entity.Payment;
import org.dsa11.team1.kumarketto.domain.entity.Transaction;
import org.dsa11.team1.kumarketto.domain.enums.DeliveryService;
import org.dsa11.team1.kumarketto.domain.enums.MessageType;
import org.dsa11.team1.kumarketto.domain.enums.PaymentStatus;
import org.dsa11.team1.kumarketto.domain.enums.TransactionStatus;
import org.dsa11.team1.kumarketto.repository.PaymentRepository;
import org.dsa11.team1.kumarketto.repository.TransactionRepository;
import org.dsa11.team1.kumarketto.websocket.WebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;
    private final ChatRoomService chatRoomService;
    private final WebSocketHandler webSocketHandler;

    /* 결제 관련 설정 */
    @Value("${stripe.webhook.secret-key}")
    private String stripeSecretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    // ... createPaymentIntent 메소드는 그대로 ...
    @Transactional
    public PaymentIntentResponseDTO createPaymentIntent(Long transactionId, Long buyerUserNo) {
        // (기존 코드와 동일)
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("거래를 찾을 수 없습니다."));
        if (!transaction.getMember().getUserNo().equals(buyerUserNo)) {
            throw new SecurityException("결제할 권한이 없습니다.");
        }
        if (transaction.getStatus() != TransactionStatus.APPROVED) {
            throw new IllegalStateException("승인된 거래만 결제할 수 있습니다.");
        }
        Payment payment = transaction.getPayment();
        if (payment == null) {
            throw new IllegalStateException("결제 정보가 생성되지 않았습니다. 거래 방식을 먼저 선택해주세요.");
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("이미 처리되었거나 진행 중인 결제입니다. 현재 상태: " + payment.getStatus());
        }
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(transaction.getProduct().getPrice().longValue())
                    .setCurrency("jpy")
                    .putMetadata("paymentId", payment.getId().toString())
                    .putMetadata("transactionId", transaction.getId().toString())
                    .build();
            PaymentIntent paymentIntent = PaymentIntent.create(params);
            return new PaymentIntentResponseDTO(paymentIntent.getClientSecret());
        } catch (StripeException e) {
            throw new RuntimeException("Stripe PaymentIntent 생성에 실패했습니다.", e);
        }
    }


    /**
     * Stripe 웹훅으로부터 결제 성공 이벤트를 처리
     */
    public void processPaymentSuccess(PaymentIntent paymentIntent) {
        long paymentId = Long.parseLong(paymentIntent.getMetadata().get("paymentId"));
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("결제 정보를 찾을 수 없습니다."));
        if (payment.getStatus() == PaymentStatus.PENDING) {
            // (DB 업데이트 및 메시지 전송 로직은 이 메소드와 아래 메소드에 동일하게 적용)
            updateStatusAndSendMessage(payment);
        }
    }

    /**
     * 로컬 테스트를 위해 수동으로 결제를 성공 처리 (수정된 최종 버전)
     */
    public void manuallyConfirmPayment(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("거래를 찾을 수 없습니다: " + transactionId));

        Payment payment = transaction.getPayment();
        if (payment == null) {
            throw new IllegalStateException("해당 거래에 대한 결제 정보가 없습니다.");
        }
        if (payment.getStatus() == PaymentStatus.PENDING) {
            // (DB 업데이트 및 메시지 전송 로직을 공통 메소드로 호출)
            updateStatusAndSendMessage(payment);
        }
    }

    /**
     * DB 상태를 업데이트하고 구매 확정 메시지를 보내는 공통 로직
     */
    private void updateStatusAndSendMessage(Payment payment) {
        // 1. DB 상태 업데이트
        payment.bindStatus(PaymentStatus.SUCCEEDED);
        payment.bindPaymentTime(LocalDateTime.now());
        Transaction transaction = payment.getTransaction();
        transaction.bindStatus(TransactionStatus.PAID);

        // 2. 메시지 전송에 필요한 정보 추출
        MemberEntity buyer = transaction.getMember();
        MemberEntity seller = transaction.getProduct().getMember();
        Long productId = transaction.getProduct().getPid();

        // 3. 채팅방 정보 조회
        ChatRoomDTO chatRoomDTO = chatRoomService.findOrCreateChatRoom(productId, buyer.getUserId());

        // 4. 메시지 내용 구성
        String messageContent = String.format("{\"transactionId\": %d}", transaction.getId());


        //  거래 방식에 따라 다른 시스템 메시지를 전송하도록 분기
        if (transaction.getDeliveryService() == DeliveryService.DELIVERY_SERVICE) {
            //  택배 거래: '판매자'에게만 '운송장 입력 요청' 메시지 전송
            ChatMessageDTO shippingRequestMessage = ChatMessageDTO.builder()
                    .chatId(chatRoomDTO.getChatId())
                    .senderId(buyer.getUserId())
                    .content(messageContent)
                    .messageType(MessageType.SHIPPING_INFO_REQUEST)
                    .build();
            // webSocketHandler.sendSystemMessage(shippingRequestMessage); // 기존 방송 메소드
            webSocketHandler.sendTargetedSystemMessage(shippingRequestMessage, seller.getUserId()); // 수정된 타겟팅 메소드

        } else {
            //  직거래: '구매자'에게만 '구매 확정 요청' 메시지 전송
            ChatMessageDTO purchaseConfirmMessage = ChatMessageDTO.builder()
                    .chatId(chatRoomDTO.getChatId())
                    .senderId(seller.getUserId())
                    .content(messageContent)
                    .messageType(MessageType.PURCHASE_CONFIRM_REQUEST)
                    .build();
            // webSocketHandler.sendSystemMessage(purchaseConfirmMessage); // 기존 방송 메소드
            webSocketHandler.sendTargetedSystemMessage(purchaseConfirmMessage, buyer.getUserId()); // 수정된 타겟팅 메소드
        }


    }
}