
package org.dsa11.team1.kumarketto.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "shipment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shipment_id")
    private Long id; // 배송 ID

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false, unique = true)
    private Transaction transaction; // 결제

    @Column(name = "courier", length = 20)
    private String courier; // 택배사

    @Column(name = "tracking_number", length = 30, unique = true)
    private String trackingNumber; // 송장번호

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt; // 배송정보입력시각

    @Column(name = "confirm_due_date")
    private LocalDateTime confirmDueDate; // 구매확정입력기한

    @Builder
    public Shipment(Transaction transaction, String courier, String trackingNumber, LocalDateTime shippedAt, LocalDateTime confirmDueDate) {
        this.transaction = transaction;
        this.courier = courier;
        this.trackingNumber = trackingNumber;
        this.shippedAt = shippedAt;
        if (shippedAt != null) this.confirmDueDate = shippedAt.plusDays(10); // 배송 시작으로부터 10일
    }

}

