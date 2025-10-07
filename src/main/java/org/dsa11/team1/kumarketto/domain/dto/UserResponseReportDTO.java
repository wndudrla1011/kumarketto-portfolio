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
public class UserResponseReportDTO {
    private String title;
    private Long pid;
    private String reportReason;
    private LocalDateTime modifiedDate;
    private String reportStatus;
    private String actionType;
}
