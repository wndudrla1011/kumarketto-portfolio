package org.dsa11.team1.kumarketto.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus {

    PENDING("決済待ち"),     // 결제 대기
    SUCCEEDED("決済成功"),   // 결제 성공
    FAILED("決済失敗"),      // 결제 실패
    CANCELED("決済キャンセル"); // 결제 취소

    private final String title;

}