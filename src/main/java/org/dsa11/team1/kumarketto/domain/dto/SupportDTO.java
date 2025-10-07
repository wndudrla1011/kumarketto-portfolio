package org.dsa11.team1.kumarketto.domain.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dsa11.team1.kumarketto.domain.entity.MemberEntity;
import org.dsa11.team1.kumarketto.domain.enums.IsPublic;
import org.dsa11.team1.kumarketto.domain.enums.Status;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SupportDTO {
    //문의ID 기본키
    private Long inquiryId;

    //회원ID

    private MemberEntity userNo;

    //제목
    private String title;

    //내용
    private String content;

    //상태
    private Status status;

    //생성시간
    private LocalDateTime createdDate;

    //수정시간
    private LocalDateTime modifiedDate;

    //공개여부
    private IsPublic isPublic;

    //파일첨부
    private String attachmentUrl;

}
