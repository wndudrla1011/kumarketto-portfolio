package org.dsa11.team1.kumarketto.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * HTML 페이지를 반환하는 전용 컨트롤러입니다.
 * @Controller 어노테이션을 사용합니다.
 */
@Controller
public class ChatPageController {

    /**
     * 사용자가 "/chat" 주소로 접속하면,
     * resources/templates/chat.html 파일을 찾아서 보여줍니다.
     * @return 보여줄 HTML 파일의 경로 (templates 폴더 기준)
     */
    @GetMapping("/chat")
    public String showChatPage() {
        return "chat"; // "chat.html"을 의미
    }

    /*
     * 참고: 이전에 만들었던 "/chat/list", "/realtimechat" 매핑은
     * 이제 "/chat" 하나로 통합되었으므로 이 컨트롤러에는 하나만 있으면 됩니다.
     */
}