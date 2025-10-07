package org.dsa11.team1.kumarketto.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.dsa11.team1.kumarketto.domain.entity.TradingReview;

import java.time.LocalDateTime;

/**
 * 작성한 리뷰 응답
 * Back -> Front
 */
@Getter
@NoArgsConstructor
public class TradingReviewResponseDTO {

    private Long reviewId; // 리뷰 ID

    private Long transactionId; // 거래 ID

    private Integer score; // 별점

    private String evaluation; // 후기

    private String authorNickname; // 작성자 닉네임

    private LocalDateTime createdAt; // 작성시간

    public TradingReviewResponseDTO(TradingReview review) {
        this.reviewId = review.getId();
        this.transactionId = review.getTransaction().getId();
        this.score = review.getScore();
        this.evaluation = review.getEvaluation();
        this.authorNickname = review.getMember().getNickname();
        this.createdAt = review.getCreatedDate();
    }

}