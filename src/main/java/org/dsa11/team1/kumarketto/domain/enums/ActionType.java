package org.dsa11.team1.kumarketto.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ActionType {
    REJECTED("却下"),
    SUSPEND_MEMBER("利用停止"),
    HIDE_POST("公開制限");
    private final String actionType;
}
