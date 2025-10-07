package org.dsa11.team1.kumarketto.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportReason {
    AD_CONTENT("広告コンテンツ"),
    PRODUCT_SPAM("商品スパム"),
    PROHIBITED_ITEM("禁止品目"),
    ABNORMAL_TRANSACTION("異常取引行為"),
    SUSPECTED_FRAUD("詐欺"),
    SENSITIVE_CONTENT("有害な内容"),
    OTHER("その他");
    private final String reportReason;
}
