package org.dsa11.team1.kumarketto.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
public class PaymentPageController {

    /**
     * 결제 팝업창(payment.html)을 렌더링합니다.
     * @return payment.html 템플릿 경로
     */
    @GetMapping({"/payment", "/payment.html"})
    public String showPaymentPage() {
        log.info("결제 페이지 템플릿 요청");
        return "transactions/payment";
    }

}
