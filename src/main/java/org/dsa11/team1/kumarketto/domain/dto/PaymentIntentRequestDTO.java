package org.dsa11.team1.kumarketto.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 요청 DTO
 * Front -> Back
 */
@Getter
@NoArgsConstructor
public class PaymentIntentRequestDTO {

    private Long transactionId; // 거래 ID

}
