package org.dsa11.team1.kumarketto.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TransactionStatus {

    PENDING("リクエスト待ち"),    // 요청 대기
    APPROVED("承認"),             // 승인
    PAID("決済完了"),             // 결제 완료
    REJECTED("拒否"),             // 거절
    CONFIRMED("取引完了");        // 거래 완료

    private final String title;

}