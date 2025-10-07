package org.dsa11.team1.kumarketto.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.dsa11.team1.kumarketto.domain.enums.TransactionStatus;

/**
 * 판매자의 '승인' 또는 '거절' 요청
 * 판매자 -> 서버
 */
@Getter
@NoArgsConstructor
public class TransactionApprovalRequestDTO {

    private TransactionStatus status; // 거래 상태

}
