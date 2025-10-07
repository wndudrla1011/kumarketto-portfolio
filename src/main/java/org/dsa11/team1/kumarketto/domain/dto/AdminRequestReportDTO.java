package org.dsa11.team1.kumarketto.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dsa11.team1.kumarketto.domain.enums.ActionType;
import org.dsa11.team1.kumarketto.domain.enums.ReportStatus;

import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class AdminRequestReportDTO {
    private Long reportId;
    private ReportStatus reportStatus;
    private ActionType actionType;
}
