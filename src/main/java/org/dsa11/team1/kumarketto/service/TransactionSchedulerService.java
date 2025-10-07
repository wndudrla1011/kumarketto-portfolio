package org.dsa11.team1.kumarketto.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsa11.team1.kumarketto.domain.entity.Transaction;
import org.dsa11.team1.kumarketto.domain.enums.TransactionStatus;
import org.dsa11.team1.kumarketto.repository.TransactionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionSchedulerService {

    private final TransactionRepository transactionRepository;

    /**
     * 자동 구매 확정 처리 (매일 자정 수행)
     */
    @Scheduled(cron = "0 0 0 * * *") // 매일 자정
    @Transactional
    public void autoCompleteTransaction() {

        log.info("구매 확정 기한이 지난 거래를 자동으로 완료 처리합니다.");

        // 확정 기한이 지났지만 아직 확정 버튼을 누르지 않은 거래들 조회
        List<Transaction> overdueTransactions = transactionRepository.findOverdueTransactions(LocalDateTime.now());

        // 확정 처리
        for (Transaction transaction : overdueTransactions) {
            log.info("거래 ID {} 자동 확정 처리.", transaction.getId());
            transaction.bindStatus(TransactionStatus.CONFIRMED);
            transaction.bindConfirmTime(LocalDateTime.now());
        }

    }

}
