package org.dsa11.team1.kumarketto.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsa11.team1.kumarketto.domain.dto.MemberRequestDTO;
import org.dsa11.team1.kumarketto.service.MemberService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/member")
@Controller
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/signIn")
    public String signIn(HttpServletRequest request, Model model) {
        String previousUrl = request.getHeader("Referer");

        if (previousUrl != null && !previousUrl.contains("/signIn")) {
            model.addAttribute("url", previousUrl);
        }

        return "signIn";
    }

    @GetMapping("/findId")
    public String findId() {return "findId";}

    @GetMapping("/resetPw")
    public String resetPw() {return "resetPw";}

    @GetMapping("/signUp")
    public String signUp(HttpServletRequest request, Model model) {
        model.addAttribute("url", request.getRequestURI());
        return "signUp";
    }

    @PostMapping("/signUp")
    public String signUp(@ModelAttribute MemberRequestDTO memberRequestDTO) {
        memberService.signUp(memberRequestDTO);
        //회원가입 완료 메시지 표시해야 함
        return "redirect:/member/signIn";
    }

}
