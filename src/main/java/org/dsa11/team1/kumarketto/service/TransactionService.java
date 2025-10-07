package org.dsa11.team1.kumarketto.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsa11.team1.kumarketto.domain.dto.ChatMessageDTO;
import org.dsa11.team1.kumarketto.domain.dto.ChatRoomDTO;
import org.dsa11.team1.kumarketto.domain.dto.TransactionApprovalRequestDTO;
import org.dsa11.team1.kumarketto.domain.dto.TransactionResponseDTO;
import org.dsa11.team1.kumarketto.domain.entity.*;
import org.dsa11.team1.kumarketto.domain.enums.*;
import org.dsa11.team1.kumarketto.repository.*;
import org.dsa11.team1.kumarketto.websocket.WebSocketHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final PaymentRepository paymentRepository;
    private final ProductElasticsearchRepository productElasticsearchRepository;
    private final WishListRepository wishListRepository;

    private final ChatRoomService chatRoomService; // ChatRoomService 주입
    private final WebSocketHandler webSocketHandler; //  WebSocketHandler 주입

    /**
     * 거래 생성
     * @param productId     거래 상품 ID
     * @param buyerUserNo   거래 요청자
     * @return TransactionRequestDTO
     */
    public TransactionResponseDTO createTransaction(Long productId, Long buyerUserNo) {

        // Entity 생성
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("상품을 찾을 수 없습니다."));

        MemberEntity buyer = memberRepository.findById(buyerUserNo)
                .orElseThrow(() -> new EntityNotFoundException("구매자를 찾을 수 없습니다."));

        MemberEntity seller = product.getMember(); //  판매자 정보 가져오기


        /* Validations */
        if (product.getMember().getUserNo().equals(buyer.getUserNo())) {
            throw new IllegalArgumentException("자신의 상품은 구매할 수 없습니다.");
        }

        if (product.getStatus() != ProductStatus.NEW) {
            throw new IllegalArgumentException("판매 중인 상품이 아닙니다.");
        }

        // Transaction 생성
        Transaction transaction = Transaction.builder()
                .product(product)
                .member(buyer)
                .requestTime(LocalDateTime.now())
                .status(TransactionStatus.PENDING)
                .deliveryService(DeliveryService.DIRECT_TRADE)
                .build();

        product.setStatus(ProductStatus.RESERVED); // 예약 중으로 상태 변경

        // 수정 사항 ES 반영
        Long likeCount = wishListRepository.countByProduct(product); // 찜 수 조회

        productElasticsearchRepository.save(ProductDocument.fromProduct(product, likeCount)); // ES 동기화

        Transaction savedTransaction = transactionRepository.save(transaction);


        // [수정] 시스템 메시지 전송 로직 추가
        ChatRoomDTO chatRoomDTO = chatRoomService.findOrCreateChatRoom(productId, buyer.getUserId());

        String messageContent = String.format(
                "{\"transactionId\": %d, \"buyerNickname\": \"%s\"}",
                savedTransaction.getId(),
                buyer.getNickname()
        );

        ChatMessageDTO systemMessage = ChatMessageDTO.builder()
                .chatId(chatRoomDTO.getChatId())
                .senderId(buyer.getUserId())
                .content(messageContent)
                .messageType(MessageType.TRANSACTION_REQUEST)
                .build();
        //모두에게 보내는 메시지
        //webSocketHandler.sendSystemMessage(systemMessage);

        // 판매자를 명시하여 타겟 메시지 전송
        webSocketHandler.sendTargetedSystemMessage(systemMessage, seller.getUserId());

        // 응답에 chatId도 포함해서 반환
        return new TransactionResponseDTO(savedTransaction, chatRoomDTO.getChatId());



        //return new TransactionResponseDTO(savedTransaction);

    }

    /**
     * 요청한 거래에 판매자 응답 반영
     * @param transactionId
     * @param sellerUserNo
     * @param approvalStatus
     * @return  TransactionResponseDTO
     */
    public TransactionResponseDTO processTransactionApproval(Long transactionId, Long sellerUserNo, TransactionStatus approvalStatus) {

        // 거래 정보 조회
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("거래를 찾을 수 없습니다."));

        Product product = transaction.getProduct();
        MemberEntity buyer = transaction.getMember();
        /* Validations */
        // 단일 사용자 테스트이므로 임시 비활성화
//        if (!product.getMember().getUserNo().equals(sellerUserNo)) {
//            throw new SecurityException("거래를 승인할 권한이 없습니다.");
//        }

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException("이미 처리된 거래입니다.");
        }

        // 요청에 따라 상태 분기
        if (approvalStatus == TransactionStatus.APPROVED) { // 요청 승인
            transaction.bindStatus(TransactionStatus.APPROVED);
            // 승인 시, 구매자에게 거래 방식 선택 시스템 메시지 전송
            ChatRoomDTO chatRoomDTO = chatRoomService.findOrCreateChatRoom(product.getPid(), buyer.getUserId());
            String messageContent = String.format("{\"transactionId\": %d}", transactionId);

            ChatMessageDTO systemMessage = ChatMessageDTO.builder()
                    .chatId(chatRoomDTO.getChatId())
                    .senderId(product.getMember().getUserId()) // 판매자가 보내는 것으로 설정
                    .content(messageContent)
                    .messageType(MessageType.TRANSACTION_TYPE_SELECT)
                    .build();
            //모두에게 보여줌
            //webSocketHandler.sendSystemMessage(systemMessage);
            // [수정 후] 구매자를 명시하여 타겟 메시지 전송

            webSocketHandler.sendTargetedSystemMessage(systemMessage, buyer.getUserId());


        } else { // 요청 거절
            transaction.bindStatus(TransactionStatus.REJECTED);
            product.setStatus(ProductStatus.NEW); // 판매 중(NEW) 상태로
            // 수정 사항 ES 반영
            Long likeCount = wishListRepository.countByProduct(product); // 찜 수 조회

            productElasticsearchRepository.save(ProductDocument.fromProduct(product, likeCount)); // ES 동기화
        }

        return new TransactionResponseDTO(transaction);

    }

    /**
     * 수동 구매 확정 처리
     * 구매자 구매 확정 버튼 클릭
     * @param transactionId     거래 ID
     * @param buyerUserNo       구매자 ID
     */
    public void confirmPurchase(Long transactionId, Long buyerUserNo) {

        // 거래 정보 조회
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("거래를 찾을 수 없습니다."));

        /* Validation */
//        if (!transaction.getMember().getUserNo().equals(buyerUserNo)) {
//            throw new SecurityException("구매 확정 권한이 없습니다.");
//        }

        if (transaction.getStatus() != TransactionStatus.PAID) {
            throw new IllegalStateException("결제가 완료된 거래만 구매 확정할 수 있습니다.");
        }

        // 구매 확정 처리
        transaction.bindStatus(TransactionStatus.CONFIRMED);
        transaction.bindConfirmTime(LocalDateTime.now());

        // 상품 상태 변경
        Product product = transaction.getProduct();
        product.setStatus(ProductStatus.SOLDOUT);


        // 구매자에게 리뷰 작성 요청 메시지 전송
        sendReviewRequestToBuyer(transaction);

        // 수정 사항 ES 반영
        Long likeCount = wishListRepository.countByProduct(product); // 찜 수 조회

        productElasticsearchRepository.save(ProductDocument.fromProduct(product, likeCount)); // ES 동기화

    }

    /**
     * 모든 참여자에게 전송하도록 변경
     * 리뷰 작성 요청 메시지를 전송하는 메소드
     */
    private void sendReviewRequestToBuyer(Transaction transaction) {
        MemberEntity buyer = transaction.getMember();
        MemberEntity seller = transaction.getProduct().getMember();
        Long productId = transaction.getProduct().getPid();

        ChatRoomDTO chatRoomDTO = chatRoomService.findOrCreateChatRoom(productId, buyer.getUserId());
        String messageContent = String.format("{\"transactionId\": %d}", transaction.getId());

        ChatMessageDTO systemMessage = ChatMessageDTO.builder()
                .chatId(chatRoomDTO.getChatId())
                .senderId(seller.getUserId())
                .content(messageContent)
                .messageType(MessageType.REVIEW_REQUEST)
                .build();

        // [수정] 구매자에게만 보내는 대신, 채팅방 전체에 방송합니다.
        // webSocketHandler.sendTargetedSystemMessage(systemMessage, buyer.getUserId());
        webSocketHandler.sendSystemMessage(systemMessage);
    }

    //구매자의 거래/결제 방식 선택을 DB에 반영하는 메소드
    /**
     * 구매자의 거래/결제 방식 선택을 DB에 반영
     * @param transactionId     거래 ID
     * @param buyerUserNo       구매자 ID
     * @param deliveryService   거래 방식 (직거래/택배)
     * @param paymentMethod     결제 방식 (카드/현금)
     * @return                  업데이트된 거래 정보
     */
    public TransactionResponseDTO updateTransactionType(Long transactionId, Long buyerUserNo, DeliveryService deliveryService, String paymentMethod) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("거래를 찾을 수 없습니다."));

        // 권한 검사: 요청한 사용자가 실제 구매자인지 확인
        if (!transaction.getMember().getUserNo().equals(buyerUserNo)) {
            throw new SecurityException("거래 방식을 선택할 권한이 없습니다.");
        }

        // 거래 방식 업데이트
        transaction.bindDeliveryService(deliveryService);
        MemberEntity buyer = transaction.getMember();
        MemberEntity seller = transaction.getProduct().getMember();
        // 현금 결제 특별 처리
        if ("CASH".equals(paymentMethod)) {
            // 결제/배송 단계를 건너뛰고 바로 '결제 완료' 상태로 변경
            transaction.bindStatus(TransactionStatus.PAID);

            // Payment 객체 생성 (현금 결제 기록)
            Payment cashPayment = Payment.builder()
                    .transaction(transaction)
                    .paymentMethod("CASH")
                    .status(PaymentStatus.SUCCEEDED)
                    .paymentTime(LocalDateTime.now()) // 현금 거래는 선택 즉시 완료로 간주
                    .build();
            paymentRepository.save(cashPayment);
            // 약속 조율 메시지를 채팅방 전체에 전송
            ChatRoomDTO chatRoomDTO = chatRoomService.findOrCreateChatRoom(transaction.getProduct().getPid(), buyer.getUserId());
            String messageContent = String.format("{\"transactionId\": %d, \"sellerId\": \"%s\"}", transaction.getId(), seller.getUserId());

            ChatMessageDTO systemMessage = ChatMessageDTO.builder()
                    .chatId(chatRoomDTO.getChatId())
                    .senderId(buyer.getUserId()) // 구매자가 선택했으므로 구매자를 sender로 설정
                    .content(messageContent)
                    .messageType(MessageType.CASH_PAYMENT_SELECTED)
                    .build();
            webSocketHandler.sendSystemMessage(systemMessage); // 채팅방 전체에 전송
        } else if ("CARD".equals(paymentMethod)) {
            // 1. 이 거래에 연결된 결제 정보가 이미 있는지 확인합니다.
            Optional<Payment> existingPaymentOpt = paymentRepository.findByTransaction(transaction);

            if (existingPaymentOpt.isPresent()) {
                Payment existingPayment = existingPaymentOpt.get();
                // 2. 이미 '결제 대기중'이거나 '성공'한 결제가 있다면, 새로 만들지 않고 그냥 넘어갑니다.
                if (existingPayment.getStatus() == PaymentStatus.PENDING || existingPayment.getStatus() == PaymentStatus.SUCCEEDED) {
                    log.info("기존 결제(상태: {})가 존재하여, 결제 생성을 건너뜁니다. Transaction ID: {}", existingPayment.getStatus(), transactionId);
                }
                // 3. '실패'한 결제가 있었다면, 지우고 새로 만들 수 있습니다. (선택적 로직)
                else if (existingPayment.getStatus() == PaymentStatus.FAILED) {
                    paymentRepository.delete(existingPayment); // 실패 기록 삭제
                    createNewCardPayment(transaction); // 새 결제 생성
                }
            } else {
                // 4. 결제 정보가 아예 없다면 새로 생성합니다.
                createNewCardPayment(transaction);
            }
        }

        return new TransactionResponseDTO(transaction);
    }

    // 카드 결제 생성 로직을 별도 메소드로 분리하여 가독성을 높입니다.
    private void createNewCardPayment(Transaction transaction) {
        Payment cardPayment = Payment.builder()
                .transaction(transaction)
                .paymentMethod("card")
                .status(PaymentStatus.PENDING) // 결제 대기 상태
                .build();
        paymentRepository.save(cardPayment);
        log.info("새로운 카드 결제(상태: PENDING)를 생성했습니다. Transaction ID: {}", transaction.getId());
    }


    @Transactional(readOnly = true)
    public TransactionResponseDTO getTransactionDetails(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("거래 정보를 찾을 수 없습니다. ID: " + transactionId));

        return new TransactionResponseDTO(transaction);
    }

    /**
     * 판매자가 상품을 전달했음을 알립니다.
     */
    public void handOverItem(Long transactionId, Long sellerUserNo) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("거래를 찾을 수 없습니다."));

        if (!transaction.getProduct().getMember().getUserNo().equals(sellerUserNo)) {
            throw new SecurityException("상품 전달을 완료할 권한이 없습니다.");
        }

        MemberEntity buyer = transaction.getMember();

        // 구매자에게 상품 수령 확인 메시지 전송
        ChatRoomDTO chatRoomDTO = chatRoomService.findOrCreateChatRoom(transaction.getProduct().getPid(), buyer.getUserId());
        String messageContent = String.format("{\"transactionId\": %d}", transaction.getId());

        ChatMessageDTO systemMessage = ChatMessageDTO.builder()
                .chatId(chatRoomDTO.getChatId())
                .senderId(transaction.getProduct().getMember().getUserId()) // 판매자가 보냄
                .content(messageContent)
                .messageType(MessageType.ITEM_RECEIVED_CHECK)
                .build();
        webSocketHandler.sendTargetedSystemMessage(systemMessage, buyer.getUserId()); // 구매자에게만 전송
    }
    /**
     * [신규] 구매자가 상품을 수령했음을 확인합니다.
     */
    public void confirmItemReceived(Long transactionId, Long buyerUserNo) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("거래를 찾을 수 없습니다."));

        if (!transaction.getMember().getUserNo().equals(buyerUserNo)) {
            throw new SecurityException("상품 수령을 확인할 권한이 없습니다.");
        }

        MemberEntity buyer = transaction.getMember();
        MemberEntity seller = transaction.getProduct().getMember();

        // [수정] 3. 구매자에게 구매 확정 요청 메시지 전송
        ChatRoomDTO chatRoomDTO = chatRoomService.findOrCreateChatRoom(transaction.getProduct().getPid(), buyer.getUserId());
        String messageContent = String.format("{\"transactionId\": %d}", transaction.getId());

        ChatMessageDTO systemMessage = ChatMessageDTO.builder()
                .chatId(chatRoomDTO.getChatId())
                .senderId(seller.getUserId()) // 시스템 메시지이므로 판매자 또는 시스템 ID로 설정
                .content(messageContent)
                .messageType(MessageType.PURCHASE_CONFIRM_REQUEST)
                .build();
        webSocketHandler.sendTargetedSystemMessage(systemMessage, buyer.getUserId()); // 구매자에게만 전송
    }


}
