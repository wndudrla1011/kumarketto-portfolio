package org.dsa11.team1.kumarketto.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.dsa11.team1.kumarketto.domain.enums.ReviewRole;

@Entity
@Table(name = "trading_reviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradingReview extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id; // 후기 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private Transaction transaction; // 거래

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_no", nullable = false)
    private MemberEntity member; // 후기 작성한 회원

    @Column(name = "score", nullable = false)
    private Integer score; // 평점


    @Column(name = "evaluation", length = 500)

    private String evaluation; // 평가 내용

    @Column(name = "image_url", length = 500)
    private String imageUrl; // 이미지 URL

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private ReviewRole role; // 구매자판매자여부

    @Builder
    public TradingReview(Transaction transaction, MemberEntity member, Integer score, String evaluation, String imageUrl, ReviewRole role) {
        this.transaction = transaction;
        this.member = member;
        this.score = score;
        this.evaluation = evaluation;
        this.imageUrl = imageUrl;
        this.role = role;
    }

}