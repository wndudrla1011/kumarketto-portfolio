package org.dsa11.team1.kumarketto.service;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsa11.team1.kumarketto.domain.dto.AdminReplyDTO;
import org.dsa11.team1.kumarketto.domain.dto.SupportDTO;
import org.dsa11.team1.kumarketto.domain.entity.AdminReplyEntity;
import org.dsa11.team1.kumarketto.domain.entity.SupportBoardEntity;
import org.dsa11.team1.kumarketto.repository.AdminReplyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@AllArgsConstructor
@Slf4j
public class AdminReplyService {
    private final AdminReplyRepository adminReplyRepository;




    /**
     * 관리자 답글 작성 서비스
     * @param dto
     */
    public void write(AdminReplyDTO dto) {
        AdminReplyEntity adminReplyEntity = new AdminReplyEntity();
        adminReplyEntity.setInquiryID(dto.getInquiryId());
        adminReplyEntity.setAnswerContent(dto.getAnswerContent());
        adminReplyRepository.save(adminReplyEntity);

//         builder 로 저장한다면 아래처럼
//        AdminReplyEntity entity = AdminReplyEntity.builder()
//                .inquiryID(dto.getInquiryID())
//                .answerContent(dto.getAnswerContent())
//                .build();
    }

    /**
     * 게시글에 대한 댓글만 담아오는 기능
     * @param inquiryId
     * @return
     */
    public List<AdminReplyDTO> findList(Long inquiryId) {
        List<AdminReplyEntity> entityList = adminReplyRepository.findByInquiryID_InquiryIdOrderByAnswerIdAsc(inquiryId);
        List<AdminReplyDTO> dtoList = new ArrayList<>();
        for (AdminReplyEntity entity : entityList){
            AdminReplyDTO dto = AdminReplyDTO.builder()
                    .answerId(entity.getAnswerId())
                    .inquiryId(entity.getInquiryID())
                    .answerContent(entity.getAnswerContent())
                    .build();
            dtoList.add(dto);
        };
        log.debug("inquiries:{}", dtoList);
        return dtoList;
    }

    /**
     * 관리자 댓글 삭제
     * @param answerId
     */
    public void delete(Long answerId) {
        adminReplyRepository.deleteById(answerId);
    }


    public void replyedit(Long answerId, String answerContent) {
        AdminReplyEntity entity = adminReplyRepository.findById(answerId).orElse(null);
        entity.setAnswerContent(answerContent);

    }
}





