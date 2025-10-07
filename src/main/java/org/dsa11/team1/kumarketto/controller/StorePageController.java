package org.dsa11.team1.kumarketto.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsa11.team1.kumarketto.repository.MemberRepository;
import org.dsa11.team1.kumarketto.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Controller
@RequiredArgsConstructor
public class StorePageController {

    private final MemberRepository memberRepository;

    /**
     * 상점 페이지 조회
     * @param userNo    상점 주인 ID
     * @return store.html
     */
    @GetMapping("/stores/{userNo}")
    public String storePage(@PathVariable Long userNo, Model model,
                            @AuthenticationPrincipal AuthenticatedUser user) {
        log.info("상점 페이지 이동");

        // 상점 주인 여부 확인
        boolean isOwner = false;
        if (user != null) {
            String ownerUserId = memberRepository.findById(userNo)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                    .getUserId();

            if (user.getUsername().equals(ownerUserId)) {
                isOwner = true;
            }
        }

        model.addAttribute("userNo", userNo);
        model.addAttribute("isOwner", isOwner);

        return "store/store";
    }

    /**
     * 상점 관리 페이지 조회
     * @param authenticatedUser 현재 로그인된 사용자 정보
     * @param model 회원 ID 전달용
     * @return 상점 관리 페이지 템플릿
     */
    @GetMapping("/my-store")
    public String myStorePage(@AuthenticationPrincipal AuthenticatedUser authenticatedUser, Model model) {

        log.info("상점 관리 페이지 이동");

        if (authenticatedUser == null) {
            return "redirect:/member/signIn";
        }

        // 로그인 회원 조회
        Long userNo = memberRepository.findByUserId(authenticatedUser.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .getUserNo();

        model.addAttribute("currentUserNo", userNo);

        return "store/my-store";

    }

}
