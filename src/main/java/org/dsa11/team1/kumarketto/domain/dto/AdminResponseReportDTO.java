package org.dsa11.team1.kumarketto.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dsa11.team1.kumarketto.domain.enums.ActionType;
import org.dsa11.team1.kumarketto.domain.enums.ReportReason;
import org.dsa11.team1.kumarketto.domain.enums.ReportStatus;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class AdminResponseReportDTO {
    private Long reportId;
    private String userId;                   // 신고한 유저
    private Long productId;                // 신고 대상
    private String title;
    private ReportReason reportReason;
    private String description;
    private LocalDateTime createdDate;
    private LocalDateTime actionDate;
    private ReportStatus reportStatus;     // 관리자가 보는 상태 (未解決, 解決済)
    private ActionType actionType;// 적용된 액션
    private String imageUrl;
}
