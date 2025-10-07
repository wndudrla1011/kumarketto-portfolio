package org.dsa11.team1.kumarketto.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제를 위한 클라이언트 시크릿 전달
 * Back -> Front
 */
@Getter
@NoArgsConstructor
public class PaymentIntentResponseDTO {

    private String clientSecret; // 결제자 확인

    public PaymentIntentResponseDTO(String clientSecret) {
        this.clientSecret = clientSecret;
    }

}
