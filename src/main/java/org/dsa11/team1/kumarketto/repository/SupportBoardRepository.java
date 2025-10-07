package org.dsa11.team1.kumarketto.repository;

import org.dsa11.team1.kumarketto.domain.entity.SupportBoardEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportBoardRepository extends JpaRepository<SupportBoardEntity,Long> {

    /**
     * 모든 문의글을 최신순으로 페이징하여 조회합니다.
     * @param pageable 페이징 정보 (페이지 번호, 페이지 크기, 정렬)
     * @return 페이징된 문의글 엔티티
     */
    Page<SupportBoardEntity> findAllByOrderByInquiryIdDesc(Pageable pageable);



    Page<SupportBoardEntity> findByUserNo_UserId(String userId, Pageable pageable);
}
