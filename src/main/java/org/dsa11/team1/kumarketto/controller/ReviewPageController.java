package org.dsa11.team1.kumarketto.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
public class ReviewPageController {

    /**
     * 거래 후기 작성 팝업창(review.html)을 렌더링합니다.
     * @return review.html 템플릿 경로
     */
    @GetMapping({"/review", "/review.html"})
    public String showReviewPage() {
        log.info("거래 후기 작성 페이지 템플릿 요청");
        // review.html 파일이 templates/transactions/ 폴더 안에 있다고 가정
        return "transactions/review";
    }
}