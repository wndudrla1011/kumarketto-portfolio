package org.dsa11.team1.kumarketto.domain.enums;

public enum MessageType {
    TEXT,
    IMAGE,
    TRANSACTION_REQUEST, //거래 요청 타입 추가
    TRANSACTION_TYPE_SELECT, //거래 방식 선택 메시지
    PAYMENT_METHOD_SELECT,    // 결제 방식 선택 메시지
    PURCHASE_CONFIRM_REQUEST, //구매 확정 요청 메세지
    SHIPPING_INFO_REQUEST, //  운송장 입력 요청 메시지 타입
    REVIEW_REQUEST, //  리뷰 작성 요청 메시지
    CASH_PAYMENT_SELECTED, //현금 결제 선택 요청 메시지
    ITEM_RECEIVED_CHECK // "상품 수령 체크"를 위한 메시지
}
