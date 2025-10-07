package org.dsa11.team1.kumarketto.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsa11.team1.kumarketto.domain.dto.TradingReviewRequestDTO;
import org.dsa11.team1.kumarketto.domain.dto.TradingReviewResponseDTO;
import org.dsa11.team1.kumarketto.domain.entity.MemberEntity;
import org.dsa11.team1.kumarketto.repository.MemberRepository;
import org.dsa11.team1.kumarketto.security.AuthenticatedUser;
import org.dsa11.team1.kumarketto.service.TradingReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/transactions/{transactionId}/reviews")
public class TradingReviewController {

    private final TradingReviewService tradingReviewService;
    private final MemberRepository memberRepository;

    /**
     * 리뷰 작성 요청
     * @param transactionId 현재 거래 ID
     * @param requestDTO    작성한 리뷰
     * @param userDetails   로그인 회원
     * @return              렌더링용 리뷰
     */
    @PostMapping
    public ResponseEntity<TradingReviewResponseDTO> createReview(
            @PathVariable Long transactionId,
            @RequestBody TradingReviewRequestDTO requestDTO,
            @AuthenticationPrincipal AuthenticatedUser userDetails
    ) {

        log.info("결제 후기 요청");

        // 로그인 상태 회원 조회
        MemberEntity member = memberRepository.findByUserId(userDetails.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("회원을 찾을 수 없습니다."));

        // 리뷰 생성
        TradingReviewResponseDTO responseDTO = tradingReviewService.createReview(transactionId, member.getUserNo(), requestDTO);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);

    }

}