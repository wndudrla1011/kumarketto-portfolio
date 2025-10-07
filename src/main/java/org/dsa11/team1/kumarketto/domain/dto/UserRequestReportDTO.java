package org.dsa11.team1.kumarketto.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dsa11.team1.kumarketto.domain.enums.ReportReason;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class UserRequestReportDTO {
    private String userId;
    private Long productId;
    private ReportReason reportReason;
    private String description;
    private String imageUrl;
}
