package org.dsa11.team1.kumarketto.domain.dto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dsa11.team1.kumarketto.domain.entity.SupportBoardEntity;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminReplyDTO {

    //답변ID
    private Long answerId;

    //문의ID
    private SupportBoardEntity inquiryId;

    //내용
    private String answerContent;
}
