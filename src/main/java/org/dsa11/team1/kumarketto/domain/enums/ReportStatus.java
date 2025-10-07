package org.dsa11.team1.kumarketto.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportStatus {
   PENDING("未解決"),
   RESOLVED("解決済");
   private final String reportStatus;
}
