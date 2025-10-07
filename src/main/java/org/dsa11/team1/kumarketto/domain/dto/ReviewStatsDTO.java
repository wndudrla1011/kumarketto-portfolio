package org.dsa11.team1.kumarketto.domain.dto;

import lombok.Getter;

/**
 * 리뷰 및 평균 별점 응답
 * Back -> Front
 */
@Getter
public class ReviewStatsDTO {

    private final long reviewCount; // 리뷰 개수

    private final double averageScore; // 평균 별점

    public ReviewStatsDTO(Long reviewCount, Double averageScore) {
        this.reviewCount = (reviewCount == null) ? 0 : reviewCount;
        this.averageScore = (averageScore == null) ? 0.0 : averageScore;
    }

}
