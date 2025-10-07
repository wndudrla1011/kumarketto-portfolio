package org.dsa11.team1.kumarketto.service;

import lombok.RequiredArgsConstructor;
import org.dsa11.team1.kumarketto.domain.dto.StoreReviewDTO;
import org.dsa11.team1.kumarketto.domain.entity.TradingReview;
import org.dsa11.team1.kumarketto.repository.TradingReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final TradingReviewRepository tradingReviewRepository;

    /**
     * 특정 상점의 리뷰(판매자 or 구매자로서) 목록을 페이징하여 조회
     *
     * @param userNo   상점 주인의 ID
     * @param hasPhoto 포토리뷰만 볼지 여부
     * @param pageable 페이징 및 정렬 정보
     * @return 페이징된 리뷰 DTO 목록
     */
    public Page<StoreReviewDTO> getReviewsByStore(Long userNo, String role, boolean hasPhoto, Pageable pageable) {
        String reviewRole = ("BUYER".equalsIgnoreCase(role)) ? "BUYER" : "SELLER";
        Page<TradingReview> reviewPage = tradingReviewRepository.findReceivedReviewsByUserNoAndRole(userNo, reviewRole, hasPhoto, pageable);
        return reviewPage.map(StoreReviewDTO::new);
    }

}
