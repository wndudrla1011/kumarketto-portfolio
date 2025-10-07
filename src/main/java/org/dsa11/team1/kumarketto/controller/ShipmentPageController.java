package org.dsa11.team1.kumarketto.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
public class ShipmentPageController {

    /**
     * 운송장 정보 입력 팝업창(shipping.html)을 렌더링합니다.
     * @return shipping.html 템플릿 경로
     */
    @GetMapping({"/shipping", "/shipping.html"})
    public String showShippingPage() {
        log.info("운송장 정보 입력 페이지 템플릿 요청");
        // shipping.html 파일이 templates/transactions/ 폴더 안에 있다고 가정
        return "transactions/shipping";
    }
}