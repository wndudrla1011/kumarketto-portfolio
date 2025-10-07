package org.dsa11.team1.kumarketto.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.dsa11.team1.kumarketto.domain.entity.Transaction;
import org.dsa11.team1.kumarketto.domain.enums.DeliveryService;

import java.time.LocalDateTime;

/**
 * 상품 구매 요청에 대한 결과 응답
 * Back -> Front
 */
@Getter
@NoArgsConstructor
public class TransactionResponseDTO {

    private Long transactionId; // 거래 ID

    private String status; // 거래 상태

    private LocalDateTime requestTime; // 결제 요청 시간

    private DeliveryService deliveryService; // 거래 방식

    private Long chatId; //chatId 필드 추가


    public TransactionResponseDTO(Transaction transaction) {
        this.transactionId = transaction.getId();
        this.status = transaction.getStatus().name();
        this.requestTime = transaction.getRequestTime();
        this.deliveryService = transaction.getDeliveryService();
    }

    //chatId를 포함하는 생성자 추가
    public TransactionResponseDTO(Transaction transaction, Long chatId) {
        this(transaction); // 기존 생성자 호출
        this.chatId = chatId;
    }

}
