package org.dsa11.team1.kumarketto.repository;

import org.dsa11.team1.kumarketto.domain.dto.TransactionHistoryDTO;
import org.dsa11.team1.kumarketto.domain.entity.Transaction;
import org.dsa11.team1.kumarketto.domain.enums.ProductStatus;
import org.dsa11.team1.kumarketto.domain.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * 자동 구매 확정
     * @param now
     * @return
     */
    @Query("SELECT t FROM Transaction t JOIN t.shipment s " +
            "WHERE t.status = 'PAID' AND s.confirmDueDate <= :now")
    List<Transaction> findOverdueTransactions(@Param("now") LocalDateTime now);

    // 특정 상품(pid)에 연결된 거래(Transaction)를 조회합니다.
    Optional<Transaction> findByProduct_Pid(Long pid);

    // 사용자의 userNo와 거래 상태를 기준으로 거래 리스트를 조회(거래 확정 시간 내림차순으로 정렬)
    List<Transaction> findByMember_UserNoAndStatusOrderByConfirmTimeDesc(Long userNo, TransactionStatus status);
    @Query("SELECT new org.dsa11.team1.kumarketto.domain.dto.TransactionHistoryDTO(" +
            "p.pid, p.name, p.price, p.imageUrl, pr.municipality.prefecture.prefName, pr.municipality.muniName, t.confirmTime, t.status) " +
            "FROM Transaction t " +
            "JOIN t.product p " +
            "LEFT JOIN p.productRegions pr " + // LEFT JOIN으로 지역 정보가 없어도 조회되도록
            "WHERE p.member.userNo = :userNo AND p.status = :status")
    List<TransactionHistoryDTO> findTransactionHistoriesByUser(
            @Param("userNo") Long userNo,
            @Param("status") ProductStatus status);




    /**
     * 채팅방 ID를 통해 해당 채팅방의 상품과 연결된 모든 거래를 조회합니다.
     * (보통 하나의 채팅방에는 하나의 거래만 연결됩니다.)
     * @param chatId 채팅방 ID
     * @return 해당 채팅방과 연관된 거래 목록
     */
    @Query("SELECT t FROM Transaction t WHERE t.product.pid = " +
            "(SELECT cr.product.pid FROM ChatRoom cr WHERE cr.chatId = :chatId) " +
            "ORDER BY t.requestTime DESC")
    List<Transaction> findTransactionsByChatId(@Param("chatId") Long chatId);

    /**
     * 채팅방 ID로 가장 최신 거래 1건만 조회합니다.
     * @param chatId 채팅방 ID
     * @return Optional<Transaction>
     */
    default Optional<Transaction> findTransactionByChatId(Long chatId) {
        return findTransactionsByChatId(chatId).stream().findFirst();
    }

}