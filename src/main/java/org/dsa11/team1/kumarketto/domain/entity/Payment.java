package org.dsa11.team1.kumarketto.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.dsa11.team1.kumarketto.domain.enums.PaymentStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id; // 결제 ID

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction; // 거래

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod; // 결제 방법

    @Column(name = "payment_time")
    private LocalDateTime paymentTime; // 결제 완료 시각

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status; // 결제 상태

    @Column(name = "shipment_due_date")
    private LocalDateTime shipmentDueDate; // 배송 정보 입력 기한

    @Builder
    public Payment(Transaction transaction, String paymentMethod, LocalDateTime paymentTime, PaymentStatus status, LocalDateTime shipmentDueDate) {
        this.transaction = transaction;
        this.paymentMethod = paymentMethod;
        this.paymentTime = paymentTime;
        this.status = status;
    }

    /* 연관 관계 메서드 */
    public void bindStatus(PaymentStatus status) {
        this.status = status;
    }

    public void bindPaymentTime(LocalDateTime paymentTime) {
        this.paymentTime = paymentTime;
        this.shipmentDueDate = paymentTime.plusDays(5);
    }

}