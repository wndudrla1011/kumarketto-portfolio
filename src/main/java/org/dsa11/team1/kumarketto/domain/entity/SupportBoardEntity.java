package org.dsa11.team1.kumarketto.domain.entity;


import jakarta.persistence.*;
import lombok.*;
import org.dsa11.team1.kumarketto.domain.enums.IsPublic;
import org.dsa11.team1.kumarketto.domain.enums.Status;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "inquiries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@EntityListeners(AuditingEntityListener.class)
public class SupportBoardEntity {
    //문의ID 기본키
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inquiry_id")
    private Long inquiryId;

    //회원ID
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_no", nullable = false)
    private MemberEntity userNo;

    //제목
    @Column(name = "title", nullable = false, length = 50)
    private String title;

    //내용
    //대용량 텍스트 이미지 사용시 Lob
    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    //상태
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    //생성시간
    @CreatedDate
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    //수정시간
    @LastModifiedDate
    @Column(name = "modified_date", updatable = false)
    private LocalDateTime modifiedDate;


    //공개여부
    @Column(name = "is_public", nullable = false)
    @Enumerated(EnumType.STRING)
    private IsPublic isPublic;

    //파일 첨부
    @Column(name = "attachment_url", length = 1000)
    private String attachmentUrl;

}
