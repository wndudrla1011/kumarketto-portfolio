package org.dsa11.team1.kumarketto.repository;

import org.dsa11.team1.kumarketto.domain.entity.AdminReplyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface AdminReplyRepository extends JpaRepository<AdminReplyEntity,Long> {
    List<AdminReplyEntity> findByInquiryID_InquiryIdOrderByAnswerIdAsc(Long inquiryId);

}
