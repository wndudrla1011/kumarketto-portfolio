package org.dsa11.team1.kumarketto.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품 구매 요청
 * Front -> Back
 */
@Getter
@NoArgsConstructor
public class TransactionRequestDTO {

    private Long productId; // 구매하려는 상품 ID

    public TransactionRequestDTO(Long productId) {
        this.productId = productId;
    }

}
