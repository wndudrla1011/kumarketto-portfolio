package org.dsa11.team1.kumarketto.repository;

import org.dsa11.team1.kumarketto.domain.entity.Prefecture;
import org.dsa11.team1.kumarketto.domain.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrefectureRepository extends JpaRepository<Prefecture, Long> {

    // Region 기준 Prefecture 리스트 조회
//    List<Prefecture> findByRegion_RgnId(Region region);
    List<Prefecture> findByRegion_RgnId(Long rgnId);
}
