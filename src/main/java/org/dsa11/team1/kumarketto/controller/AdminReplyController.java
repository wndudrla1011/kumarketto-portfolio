package org.dsa11.team1.kumarketto.controller;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsa11.team1.kumarketto.domain.dto.AdminReplyDTO;
import org.dsa11.team1.kumarketto.domain.dto.SupportDTO;
import org.dsa11.team1.kumarketto.domain.entity.AdminReplyEntity;
import org.dsa11.team1.kumarketto.domain.entity.SupportBoardEntity;
import org.dsa11.team1.kumarketto.repository.AdminReplyRepository;
import org.dsa11.team1.kumarketto.service.AdminReplyService;
import org.dsa11.team1.kumarketto.service.SupportBoardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@AllArgsConstructor
@Slf4j
public class AdminReplyController {
    private final AdminReplyService adminReplyService;
    private final AdminReplyRepository adminReplyRepository;
    private final SupportBoardService supportBoardService;



    /**
     * 관리자 답글 작성 기능
     * @param model 필요없는값임
     * @param dto 기본값 게시판 글번호, 글 내용
     * @return
     */
    @PostMapping("inquires/detail/adminreply")
    public String adminreply(Model model,
                             @RequestParam("inquiryId") Long inquiryId,
                             @ModelAttribute AdminReplyDTO dto){

        log.debug("관리자 답글: {}", dto);
        adminReplyService.write(dto);

        return "redirect:/inquires/detail/" + inquiryId;
    }


    //댓글 수정하기
    @PostMapping("/inquires/detail/adminreply/{inquiryId}/edit-{answerId}")
    public String beginEdit(@PathVariable("inquiryId") Long inquiryId,
                            @PathVariable("answerId") Long answerId,
                            @RequestParam("answerContent") String answerContent) {


        adminReplyService.replyedit(answerId,answerContent);
        return "redirect:/inquires/detail/" + inquiryId;
    }


    /**
     * 댓글 삭제 기능
     * @param inquiryId
     * @param answerId
     * @return
     */
    @GetMapping("/inquires/detail/adminreply/{inquiryId}/delete-{answerId}")
    public String deleteReply(
            @PathVariable("inquiryId") Long inquiryId,
            @PathVariable("answerId") Long answerId) {
        adminReplyService.delete(answerId);
        return "redirect:/inquires/detail/" + inquiryId;
    }

}


