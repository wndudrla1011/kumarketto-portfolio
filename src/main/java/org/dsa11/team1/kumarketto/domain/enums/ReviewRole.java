
package org.dsa11.team1.kumarketto.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReviewRole {

    BUYER("購入者"),   // 구매자
    SELLER("販売者");  // 판매자

    private final String title;

}
