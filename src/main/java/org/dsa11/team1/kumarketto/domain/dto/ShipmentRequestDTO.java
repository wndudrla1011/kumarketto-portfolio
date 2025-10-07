package org.dsa11.team1.kumarketto.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 판매자가 입력할 배송 정보
 * Front -> Back
 */
@Getter
@NoArgsConstructor
public class ShipmentRequestDTO {

    private String courier; // 택배사

    private String trackingNumber; // 송장번호

}
