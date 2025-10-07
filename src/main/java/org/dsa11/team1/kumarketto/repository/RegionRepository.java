package org.dsa11.team1.kumarketto.repository;

import org.dsa11.team1.kumarketto.domain.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RegionRepository extends JpaRepository<Region, Long> {

    @Query("SELECT DISTINCT r FROM Region r JOIN FETCH r.prefectures p JOIN FETCH p.municipalities")
    List<Region> findAllWithPrefecturesAndMunicipalities();

}
