package org.dsa11.team1.kumarketto.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.dsa11.team1.kumarketto.domain.entity.MemberEntity;
import org.dsa11.team1.kumarketto.domain.entity.TradingReview;
import org.dsa11.team1.kumarketto.domain.entity.Transaction;
import org.dsa11.team1.kumarketto.domain.enums.ReviewRole;

/**
 * 리뷰 작성 DTO
 * Front -> Back
 */
@Getter
@NoArgsConstructor
public class TradingReviewRequestDTO {

    private Integer score; // 별점

    private String evaluation; // 후기 내용

    public TradingReview toEntity(Transaction transaction, MemberEntity member, ReviewRole role) {
        return TradingReview.builder()
                .transaction(transaction)
                .member(member)
                .score(this.score)
                .evaluation(this.evaluation)
                .role(role)
                .build();
    }

}