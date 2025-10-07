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
public class UserInquiryDTO {
    //문의ID 기본키
    private Long inquiryId;

    //제목
    private String title;

    //상태
    private String status;

    //생성시간
    private LocalDateTime createdDate;

    //공개여부
    private String isPublic;
}
