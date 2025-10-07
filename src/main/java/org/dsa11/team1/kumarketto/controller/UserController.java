package org.dsa11.team1.kumarketto.controller;

import org.dsa11.team1.kumarketto.security.AuthenticatedUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
//실시간 채팅에서 본인인증위한 컨트롤러입니다
public class UserController {

    /**
     * 현재 로그인된 사용자의 ID를 반환하는 API 입니다.
     * @param authenticatedUser Spring Security가 자동으로 넣어주는 로그인 사용자 정보
     * @return {"userId": "현재사용자ID"} 형태의 JSON 데이터
     */
    @GetMapping("/api/user/me")
    public ResponseEntity<Map<String, String>> getCurrentUser(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        // 로그인하지 않은 사용자가 이 API를 요청하면 authenticatedUser는 null이 됩니다.
        // Spring Security 설정에 따라 이 주소는 인증된 사용자만 접근 가능해야 합니다.
        if (authenticatedUser == null) {
            // 이 경우는 보통 발생하지 않지만, 안정성을 위해 처리
            return ResponseEntity.status(401).build(); // 401 Unauthorized
        }
        String userId = authenticatedUser.getUsername();
        Map<String, String> response = Collections.singletonMap("userId", userId);
        return ResponseEntity.ok(response);
    }
}