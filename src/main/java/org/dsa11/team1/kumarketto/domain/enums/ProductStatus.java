package org.dsa11.team1.kumarketto.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductStatus {

    NEW("新規"),              // 신규
    RESERVED("取引中"),       // 거래중
    SOLDOUT("販売完了"),      // 판매완료
    REPORTED("通報商品");     // 신고상품

    private final String title;

}
