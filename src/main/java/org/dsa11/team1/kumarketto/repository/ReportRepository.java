package org.dsa11.team1.kumarketto.repository;
import org.dsa11.team1.kumarketto.domain.enums.ActionType;
import org.dsa11.team1.kumarketto.domain.enums.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.dsa11.team1.kumarketto.domain.entity.ReportsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<ReportsEntity, Long> {
    List<ReportsEntity> findAllByOrderByCreatedDateDesc();
    List<ReportsEntity> findAllByProduct_Pid(Long productId);
    Page<ReportsEntity> findByMemberEntity_UserId(String userId, Pageable pageable);
    Page<ReportsEntity> findByProduct_Member_UserIdAndActionResultsEntity_ActionTypeNot(String userId, ActionType actionType, Pageable pageable);
    Page<ReportsEntity> findByReportStatus(ReportStatus reportStatus, Pageable pageable);
}
