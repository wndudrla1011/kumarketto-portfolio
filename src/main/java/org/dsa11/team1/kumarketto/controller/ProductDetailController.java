package org.dsa11.team1.kumarketto.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsa11.team1.kumarketto.domain.dto.ProductDetailDTO;
import org.dsa11.team1.kumarketto.domain.entity.MemberEntity;
import org.dsa11.team1.kumarketto.repository.MemberRepository;
import org.dsa11.team1.kumarketto.security.AuthenticatedUser;
import org.dsa11.team1.kumarketto.service.ProductDetailService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("product")
public class ProductDetailController {

    private final ProductDetailService productDetailService;
    private final MemberRepository memberRepository;

    /**
     * 상품 상세 페이지 조회
     * @param productId 조회할 상품의 ID
     * @param model     뷰에 데이터를 전달할 모델 객체
     * @param user      현재 로그인한 사용자 정보 (Spring Security가 자동으로 주입)
     * @return 상품 상세 페이지 뷰 경로
     */
    @GetMapping("/detail/{productId}")
    public String productDetail(@PathVariable("productId") Long productId,
                                @AuthenticationPrincipal AuthenticatedUser user
            , Model model) {

        // 조회수 증가 (트랜잭션 있는 서비스 메서드 호출)
        productDetailService.incrementViewCount(productId);

        // 1. productId를 사용해 서비스에서 상품 상세 정보 DTO를 받아옵니다.
        String userNo = (user != null) ? user.getUsername() : null;
        ProductDetailDTO productDTO = productDetailService.getProductDetail(productId, userNo);

        // 2. 받아온 상품 정보를 모델에 담아 뷰로 전달합니다.
        model.addAttribute("product", productDTO);

        // 3. 보여줄 뷰의 이름을 반환합니다.
        return "/products/productDetail";

    }

    /**
     * 찜 기능
     * @param productId 상품 ID
     * @param user 로그인한 유저
     * @return
     */
    @PostMapping("like")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> like(@RequestParam Long productId,
                                                    @AuthenticationPrincipal AuthenticatedUser user) {

        // 서비스 메서드 호출, 반환 값은 찜 개수
        boolean isWished = productDetailService.wish(user.getUserNo(), productId);

        // JSON 형식으로 응답 데이터 생성
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        // 찜 개수가 0보다 크면 'true', 아니면 'false'로 찜 상태를 판단
        response.put("isWished", isWished);

        return ResponseEntity.ok(response);

    }


}
