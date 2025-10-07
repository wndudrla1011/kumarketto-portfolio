package org.dsa11.team1.kumarketto.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dsa11.team1.kumarketto.domain.enums.ReportReason;
import org.dsa11.team1.kumarketto.domain.enums.ReportStatus;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = "reports")
public class ReportsEntity {

    @Id
    @Column(name = "report_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_no", nullable = false)
    private MemberEntity memberEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_reason", nullable = false)
    private ReportReason reportReason;

    @Column(name = "description")
    private String description;

    @Builder.Default
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    @Builder.Default
    @Column(name = "modified_date", nullable = false)
    private LocalDateTime modifiedDate = LocalDateTime.now();

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "report_status", nullable = false)
    private ReportStatus reportStatus = ReportStatus.PENDING;

    @OneToOne(mappedBy = "reportsEntity", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private ActionResultsEntity actionResultsEntity;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

}
