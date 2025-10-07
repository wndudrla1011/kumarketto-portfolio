package org.dsa11.team1.kumarketto.domain.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inquiries_answers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class AdminReplyEntity {

    //답변ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answer_id")
    private Long answerId;

    //문의ID
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inquiry_id", nullable = false)
    private SupportBoardEntity inquiryID;


    //내용
    @Lob
    @Column(name = "answer_content", nullable = false)
    private String answerContent;

}
