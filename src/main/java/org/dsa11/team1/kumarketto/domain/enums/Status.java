package org.dsa11.team1.kumarketto.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Status {
    PENDING("対応前"), //처리중
    COMPLETED("解決"); //답변완료


    private final String title;
}
