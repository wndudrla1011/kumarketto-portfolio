package org.dsa11.team1.kumarketto.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DeliveryService {

    DELIVERY_SERVICE("宅配取引"),  // 택배거래
    DIRECT_TRADE("直接取引");      // 직거래

    private final String title;

}