package org.dsa11.team1.kumarketto.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.dsa11.team1.kumarketto.domain.dto.TradingReviewRequestDTO;
import org.dsa11.team1.kumarketto.domain.dto.TradingReviewResponseDTO;
import org.dsa11.team1.kumarketto.domain.entity.MemberEntity;
import org.dsa11.team1.kumarketto.domain.entity.TradingReview;
import org.dsa11.team1.kumarketto.domain.entity.Transaction;
import org.dsa11.team1.kumarketto.domain.enums.ReviewRole;
import org.dsa11.team1.kumarketto.domain.enums.TransactionStatus;
import org.dsa11.team1.kumarketto.repository.MemberRepository;
import org.dsa11.team1.kumarketto.repository.TradingReviewRepository;
import org.dsa11.team1.kumarketto.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TradingReviewService {

    private final TradingReviewRepository tradingReviewRepository;
    private final TransactionRepository transactionRepository;
    private final MemberRepository memberRepository;

    public TradingReviewResponseDTO createReview(Long transactionId, Long userNo, TradingReviewRequestDTO requestDTO) {

        // 리뷰 작성자 조회
        MemberEntity author = memberRepository.findById(userNo)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        // 거래 정보 조회
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("거래를 찾을 수 없습니다."));

        /* Validation */
        validateReviewCreation(transaction, author);

        // 리뷰 작성자의 역할 결정
        ReviewRole role = transaction.getMember().equals(author) ? ReviewRole.BUYER : ReviewRole.SELLER;

        // DTO -> Entity
        TradingReview review = requestDTO.toEntity(transaction, author, role);
        TradingReview savedReview = tradingReviewRepository.save(review);

        return new TradingReviewResponseDTO(savedReview);

    }

    private void validateReviewCreation(Transaction transaction, MemberEntity author) {

        // 거래가 CONFIRMED 상태일 때만 리뷰 작성 가능
        if (transaction.getStatus() != TransactionStatus.CONFIRMED) {
            throw new IllegalStateException("완료된 거래에 대해서만 후기를 작성할 수 있습니다.");
        }

        // 거래 당사자만 리뷰 작성 가능
        boolean isBuyer = transaction.getMember().equals(author);
        boolean isSeller = transaction.getProduct().getMember().equals(author);

        if (!isBuyer && !isSeller) {
            throw new SecurityException("거래 당사자만 후기를 작성할 수 있습니다.");
        }

        // 동일한 거래에 대해 동일한 사용자가 리뷰 중복 작성 방지
        ReviewRole role = isBuyer ? ReviewRole.BUYER : ReviewRole.SELLER;
        if (tradingReviewRepository.existsByTransactionAndMemberAndRole(transaction, author, role)) {
            throw new IllegalStateException("이미 해당 거래에 대한 후기를 작성했습니다.");
        }

    }

}