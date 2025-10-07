package org.dsa11.team1.kumarketto.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.dsa11.team1.kumarketto.domain.entity.TradingReview;

import java.time.LocalDateTime;

/**
 * 리뷰 카드 생성 DTO
 * Back -> Front
 */
@Getter
@NoArgsConstructor
public class StoreReviewDTO {
    private Long reviewId;              // 리뷰 ID
    private String reviewerNickname;    // 리뷰 작성자 닉네임
    private String reviewerImageUrl;    // 리뷰 작성자 프로필 이미지
    private int score;                  // 별점
    private String content;             // 리뷰 내용
    private String productName;         // 어떤 상품에 대한 리뷰인지
    private LocalDateTime createdDate;  // 리뷰 작성일

    public StoreReviewDTO(TradingReview review) {
        this.reviewId = review.getId();
        this.score = review.getScore();
        this.content = review.getEvaluation();
        this.createdDate = review.getCreatedDate();

        if (review.getMember() != null) {
            this.reviewerNickname = review.getMember().getNickname();

            // 작성자의 Store 정보가 있을 경우 프로필 이미지 가져오기
            if (review.getMember().getStore() != null) {
                this.reviewerImageUrl = review.getMember().getStore().getProfileImage();
            } else {
                this.reviewerImageUrl = null; // 기본 이미지 처리는 프론트에서
            }
        } else {
            this.reviewerNickname = "알 수 없는 사용자";
            this.reviewerImageUrl = null;
        }

        // 상품 정보
        if (review.getTransaction() != null && review.getTransaction().getProduct() != null) {
            this.productName = review.getTransaction().getProduct().getName();
        } else {
            this.productName = "알 수 없는 상품";
        }
    }
}
