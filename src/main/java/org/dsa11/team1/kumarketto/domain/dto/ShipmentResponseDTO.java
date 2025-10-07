package org.dsa11.team1.kumarketto.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.dsa11.team1.kumarketto.domain.entity.Shipment;

import java.time.LocalDateTime;

/**
 * 배송 정보 등록 후 클라이언트로 응답
 * Back -> Front
 */
@Getter
@NoArgsConstructor
public class ShipmentResponseDTO {

    private Long shipmentId; // 배송 ID

    private Long transactionId; // 거래 ID

    private String courier; // 배송사

    private String trackingNumber; // 송장번호

    private LocalDateTime shippedAt; // 배송정보 입력 시각

    private LocalDateTime comfirmDueDate; // 구매확정 입력 기한

    public ShipmentResponseDTO(Shipment shipment) {
        this.shipmentId = shipment.getId();
        this.transactionId = shipment.getTransaction().getId();
        this.courier = shipment.getCourier();
        this.trackingNumber = shipment.getTrackingNumber();
        this.shippedAt = shipment.getShippedAt();
        this.comfirmDueDate = shipment.getConfirmDueDate();
    }

}
