package org.dsa11.team1.kumarketto.repository;

import org.dsa11.team1.kumarketto.domain.dto.ReviewStatsDTO;
import org.dsa11.team1.kumarketto.domain.entity.MemberEntity;
import org.dsa11.team1.kumarketto.domain.entity.TradingReview;
import org.dsa11.team1.kumarketto.domain.entity.Transaction;
import org.dsa11.team1.kumarketto.domain.enums.ReviewRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TradingReviewRepository extends JpaRepository<TradingReview, Long> {

    /**
     * 특정 사용자(상점 주인)가 받은 모든 리뷰를 페이징하여 조회
     * @param userNo 사용자의 user_no (PK)
     * @param role   리뷰 작성자의 역할
     * @param hasPhoto true 일 경우 포토리뷰만 조회 (imageUrl이 NULL이 아닌 경우)
     * @param pageable 페이징 및 정렬 정보 (최신순, 별점순 등 정렬 포함)
     * @return 페이징된 리뷰 목록
     */
    @Query("""
    SELECT r FROM TradingReview r
    JOIN FETCH r.member
    JOIN r.transaction t
    JOIN t.product p
    WHERE ((:role = 'SELLER' AND p.member.userNo = :userNo) OR (:role = 'BUYER' AND t.member.userNo = :userNo))
      AND r.member.userNo != :userNo
      AND (:hasPhoto = false OR r.imageUrl IS NOT NULL)
""")
    Page<TradingReview> findReceivedReviewsByUserNoAndRole(@Param("userNo") Long userNo,
                                                           @Param("role") String role,
                                                           @Param("hasPhoto") boolean hasPhoto,
                                                           Pageable pageable);

    /**
     * 특정 판매자의 리뷰 개수와 평균 별점을 조회
     * @param sellerUserNo 판매자의 user_no (PK)
     * @return 리뷰 통계 정보를 담은 DTO
     */
    @Query("""
    SELECT new org.dsa11.team1.kumarketto.domain.dto.ReviewStatsDTO(
        COUNT(r),
        AVG(r.score)
    )
    FROM TradingReview r
    WHERE r.transaction.product.member.userNo = :sellerUserNo
    """)
    ReviewStatsDTO getReviewStatsBySellerUserNo(@Param("sellerUserNo") Long sellerUserNo);

    /**
     * 중복 리뷰 작성 방지
     * @param transaction   진행한 거래
     * @param member        거래 당사자
     * @param role          구매자 or 판매자
     * @return              true or false
     */
    boolean existsByTransactionAndMemberAndRole(Transaction transaction, MemberEntity member, ReviewRole role);

}