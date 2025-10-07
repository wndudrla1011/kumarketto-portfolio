package org.dsa11.team1.kumarketto.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.dsa11.team1.kumarketto.domain.dto.SupportDTO;
import org.dsa11.team1.kumarketto.domain.dto.UserInquiryDTO;
import org.dsa11.team1.kumarketto.security.AuthenticatedUser;
import org.dsa11.team1.kumarketto.service.MemberService;
import org.dsa11.team1.kumarketto.service.SupportBoardService;

import org.dsa11.team1.kumarketto.domain.dto.UserResponseReportDTO;

import org.dsa11.team1.kumarketto.service.ReportService;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/myPage")
@RestController
public class MyPageAPIController {

    private final BCryptPasswordEncoder passwordEncoder;
    private final MemberService memberService;
    private final SupportBoardService supportBoardService;
    private final ReportService reportService;


    @PostMapping("/profile/verify")
    public Map<String, Object> verifyPassword(@RequestBody Map<String, String> payload, @AuthenticationPrincipal AuthenticatedUser user) {
        String pw = payload.get("password");

        boolean verified = passwordEncoder.matches(pw, user.getPassword());
        Map<String, Object> response = new HashMap<>();
        if(verified) {
            response.put("userId", user.getUsername());
        }
        return response;
    }

    @PostMapping("/profile/checkPassword")
    public Map<String, Object> checkPassword(@RequestBody Map<String, String> payload, @AuthenticationPrincipal AuthenticatedUser user) {
        String newPwd = payload.get("newPassword");

        boolean same = passwordEncoder.matches(newPwd, user.getPassword());
        Map<String, Object> response = new HashMap<>();
        response.put("same", same);
        return response;
    }

    @PostMapping("/withdraw")
    public Map<String, Object> withdraw(@RequestBody Map<String, String> payload,
                                        @AuthenticationPrincipal AuthenticatedUser user,
                                        HttpServletResponse response,
                                        HttpServletRequest request) {
        Map<String, Object> responseMap = new HashMap<>();
        String inputPwd = payload.get("password");
        String curPwd = user.getPassword();

        boolean match = passwordEncoder.matches(inputPwd, curPwd);
        if(!match) {
            responseMap.put("match", false);
            return responseMap;
        }
        memberService.disableUser(user);
        new SecurityContextLogoutHandler().logout(request, response, null);

        responseMap.put("match", true);
        return responseMap;
    }

    @GetMapping("/inquiries/history")
    public ResponseEntity<Map<String, Object>> getUserInquiries(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        String userId = authenticatedUser.getUsername();
        Page<UserInquiryDTO> userInquiries = supportBoardService.getUserInquiries(userId, page, size);
        Map<String, Object> response = new HashMap<>();
        response.put("content", userInquiries.getContent());
        response.put("last", userInquiries.isLast());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reports/report")
    public ResponseEntity<Map<String, Object>> getReports(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        String userId = authenticatedUser.getUsername();
        Page<UserResponseReportDTO> userResponseReportDTOList = reportService.getReports(page, size, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("content", userResponseReportDTOList.getContent());
        response.put("last", userResponseReportDTOList.isLast());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/reports/sanction")
    public ResponseEntity<Map<String, Object>> getSanctions(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        String userId = authenticatedUser.getUsername();
        Page<UserResponseReportDTO> userResponseReportDTOList = reportService.getSanctions(page, size, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("content", userResponseReportDTOList.getContent());
        response.put("last", userResponseReportDTOList.isLast());

        return ResponseEntity.ok(response);
    }
}
