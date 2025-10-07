package org.dsa11.team1.kumarketto.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.dsa11.team1.kumarketto.domain.enums.DeliveryService;
import org.dsa11.team1.kumarketto.domain.enums.TransactionStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "transaction")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long id; // 거래 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product; // 상품

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_no", nullable = false)
    private MemberEntity member; // 회원

    @OneToOne(mappedBy = "transaction", fetch = FetchType.LAZY)
    private Payment payment; // 결제

    @Column(name = "request_time", nullable = false)
    private LocalDateTime requestTime; // 요청 시간

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status; // 거래 상태

    @Column(name = "confirm_time")
    private LocalDateTime confirmTime; // 거래확정시각

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type")
    private DeliveryService deliveryService; // 거래 방식

    @Column(name = "payment_due_date", nullable = false)
    private LocalDateTime paymentDueDate; // 거래완료기한

    @OneToOne(mappedBy = "transaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Shipment shipment; // 배송

    @OneToMany(mappedBy = "transaction")
    private List<TradingReview> reviews = new ArrayList<>(); // 거래 후기

    @Builder
    public Transaction(Product product, MemberEntity member, LocalDateTime requestTime, TransactionStatus status, DeliveryService deliveryService, LocalDateTime paymentDueDate) {
        this.product = product;
        this.member = member;
        this.requestTime = requestTime;
        this.status = status;
        this.deliveryService = deliveryService;

        if (requestTime != null) this.paymentDueDate = requestTime.plusHours(12);
    }

    /* 연관 관계 메서드 */
    public void bindShipment(Shipment shipment) {
        this.shipment = shipment;
    }

    public void bindStatus(TransactionStatus status) {
        this.status = status;
    }

    public void bindConfirmTime(LocalDateTime confirmTime) {
        this.confirmTime = confirmTime;
    }


    public void bindDeliveryService(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

}