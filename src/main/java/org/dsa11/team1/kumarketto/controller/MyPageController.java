package org.dsa11.team1.kumarketto.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.dsa11.team1.kumarketto.domain.dto.MemberRequestDTO;
import org.dsa11.team1.kumarketto.domain.dto.MemberResponseDTO;
import org.dsa11.team1.kumarketto.domain.dto.ProductDTO;
import org.dsa11.team1.kumarketto.domain.dto.TransactionHistoryDTO;
import org.dsa11.team1.kumarketto.domain.entity.MemberEntity;
import org.dsa11.team1.kumarketto.domain.entity.Product;
import org.dsa11.team1.kumarketto.security.AuthenticatedUser;
import org.dsa11.team1.kumarketto.service.MemberService;
import org.dsa11.team1.kumarketto.service.MyPageService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RequestMapping("/myPage")
@Controller
public class MyPageController {

    private final MemberService memberService;
    private final MyPageService mypageService;

    // /myPage 접속 시 기본 거래 내역 페이지로 리다이렉트
    @GetMapping
    public String redirectToTransactions(HttpServletRequest request, Model model) {
        model.addAttribute("url", request.getRequestURI());
        return "redirect:/myPage/transactions";
    }

    // 거래 내역 페이지
    @GetMapping("/transactions")
    public String transactions(HttpServletRequest request,
                               Authentication authentication,
                               Model model) {
        model.addAttribute("url", request.getRequestURI());
        AuthenticatedUser authenticatedUser = (AuthenticatedUser) authentication.getPrincipal();
        //판매리스트 관련
        MemberEntity memberEntity = memberService.findMemberByUserId(authenticatedUser.getUsername()); // 멤버조회
        Long user = memberEntity.getUserNo();

        // 판매 완료 상품 리스트를 가져와 모델에 추가
        List<TransactionHistoryDTO> salesList = mypageService.getSoldOut(user);
        model.addAttribute("salesList", salesList);

        // 구매 완료 상품 리스트를 가져와 모델에 추가
        List<TransactionHistoryDTO> purchaseList = mypageService.getConfirmed(user);
        model.addAttribute("purchaseList", purchaseList);
        model.addAttribute("loginUserNo", user);

        return "mypage/transactions"; // view: src/main/resources/templates/myPage/transactions.html
    }

    @GetMapping("/profile")
    public String profile(HttpServletRequest request, Authentication authentication, Model model) {
        model.addAttribute("url", request.getRequestURI());
        AuthenticatedUser authenticatedUser = (AuthenticatedUser) authentication.getPrincipal();

        MemberResponseDTO memberResponseDTO =
                memberService.findUserById(authenticatedUser.getUsername());

        model.addAttribute("user", memberResponseDTO);

        return "mypage/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@ModelAttribute MemberRequestDTO memberRequestDTO,
                                @AuthenticationPrincipal AuthenticatedUser user) {
        memberService.updateProfile(user.getUsername(), memberRequestDTO);

        return "redirect:/myPage";
    }

    @GetMapping("/withdraw")
    public String withdraw(HttpServletRequest request, Model model) {
        model.addAttribute("url", request.getRequestURI());
        return "mypage/withdraw";
    }

    @GetMapping("/inquiries")
    public String inquires(HttpServletRequest request, Model model) {
        model.addAttribute("url", request.getRequestURI());
        return "mypage/inquiries";
    }

    @GetMapping("/favorites")
    public String wishilist(HttpServletRequest request,
                            Authentication authentication,
                            Model model) {
        model.addAttribute("url", request.getRequestURI());
        AuthenticatedUser authenticatedUser = (AuthenticatedUser) authentication.getPrincipal();

        MemberEntity memberEntity = memberService.findMemberByUserId(authenticatedUser.getUsername()); // 멤버조회
        Long user = memberEntity.getUserNo();

        List<Product> wishlist = mypageService.getWishlistItems(user);

        List<ProductDTO> wishlistDTO = wishlist.stream()
                .map(product -> ProductDTO.builder()
                        .pid(product.getPid())
                        .name(product.getName())
                        .price(product.getPrice())
                        .imageUrl(product.getImageUrl())
                        .regionName(
                                product.getProductRegions().stream().findFirst()
                                        .map(productRegion -> {
                                            // Municipality와 Prefecture 이름을 조합
                                            String prefectureName = productRegion.getMunicipality().getPrefecture().getPrefName();
                                            String municipalityName = productRegion.getMunicipality().getMuniName();
                                            return prefectureName + " · " + municipalityName;
                                        })
                                        // 지역 정보가 없는 경우 null 또는 빈 문자열 반환
                                        .orElse(null)
                        )
                        .build())

                .collect(Collectors.toList());

        model.addAttribute("wishlistItems", wishlistDTO);

        return "mypage/favorites"; //찜목록페이지 이동

    }

    @GetMapping("/sales")
    public String sales(Authentication authentication, Model model) {
        AuthenticatedUser authenticatedUser = (AuthenticatedUser) authentication.getPrincipal();
        Long user = memberService.findMemberByUserId(authenticatedUser.getUsername()).getUserNo();

        // 판매 목록 전체를 가져오는 서비스 메서드 호출
        List<TransactionHistoryDTO> soldOutList = mypageService.getSoldOut(user);

        model.addAttribute("salesList", soldOutList);

        return "mypage/sales"; // 뷰 이름
    }

    @GetMapping("/purchases")
    public String purchases(Authentication authentication, Model model) {
        AuthenticatedUser authenticatedUser = (AuthenticatedUser) authentication.getPrincipal();
        Long user = memberService.findMemberByUserId(authenticatedUser.getUsername()).getUserNo();

        // 구매 목록 전체를 가져오는 서비스 메서드 호출
        List<TransactionHistoryDTO> confirmedList = mypageService.getConfirmed(user);

        model.addAttribute("purchaseList", confirmedList);

        return "mypage/purchases"; // 뷰 이름

    }

    @GetMapping("/reports")
    public String reports(HttpServletRequest request, Model model) {
        model.addAttribute("url", request.getRequestURI());
        return "mypage/reports";
    }
}
