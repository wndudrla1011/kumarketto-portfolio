package org.dsa11.team1.kumarketto.domain.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 상점 통계용
 * Back -> Front
 */
@Getter
@Builder
public class StoreStatsDTO {

    private final long totalProductCount; // 판매자의 전체 상품 수

    private final long transactionCount; // 판매 완료된 상품 수

    private final long reviewCount; // 리뷰 개수

    private final double averageRating; // 평균 별점

}
