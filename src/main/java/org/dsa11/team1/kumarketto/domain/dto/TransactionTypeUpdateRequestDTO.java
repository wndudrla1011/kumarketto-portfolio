package org.dsa11.team1.kumarketto.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.dsa11.team1.kumarketto.domain.enums.DeliveryService;

/**
 * 구매자의 거래 방식 선택 전송
 * Back -> Front
 */
@Getter
@NoArgsConstructor
public class TransactionTypeUpdateRequestDTO {

    private DeliveryService deliveryService; // 거래 방식
    private String paymentMethod; // "CARD" 또는 "CASH"

}
