package org.dsa11.team1.kumarketto.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.dsa11.team1.kumarketto.domain.dto.MemberRequestDTO;
import org.dsa11.team1.kumarketto.domain.entity.MemberEntity;
import org.dsa11.team1.kumarketto.repository.MemberRepository;
import org.dsa11.team1.kumarketto.service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Member;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@RequestMapping("/member")
@RestController
public class MemberAPIController {

    public final MemberService memberService;
    public final MemberRepository memberRepository;

    @PostMapping("/findId")
    public Map<String, String> findId(@RequestParam("inputEmail") String email) {
        String userId = memberService.findIdByEmail(email);
        Map<String, String> result = new HashMap<>();
        result.put("userId", userId);
        return result;
    }

    @PostMapping("/checkUser")
    public boolean checkUser(@ModelAttribute MemberRequestDTO memberRequestDTO){
        return memberService.existsByIdAndBirth(memberRequestDTO);
    }

    @PostMapping("/resetPw")
    public String resetPw(@RequestParam("userId") String userId,
                          @RequestParam("password") String password) {
        memberService.resetPw(userId, password);
        return "success";
    }

    @PostMapping("/userIdDupCheck")
    public boolean userIdDupCheck(@RequestParam("userId") String userId) {
        return memberService.existsByUserIdAndEnabledTrue(userId);
    }

    @PostMapping("/emailDupCheck")
    public boolean emailDupCheck(@RequestParam("email") String email) {
        return memberService.existsByEmail(email);
    }

    @PostMapping("/nicknameDupCheck")
    public boolean nicknameDupCheck(@RequestParam("nickname") String nickname) {
        return memberService.existsByNickname(nickname);
    }

}
