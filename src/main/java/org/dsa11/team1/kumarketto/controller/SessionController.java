package org.dsa11.team1.kumarketto.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/session")
public class SessionController {

    @PostMapping("/clearPrevPage")
    public void clearPrevPage(HttpSession session) {
        session.removeAttribute("prevPage");
    }
}
