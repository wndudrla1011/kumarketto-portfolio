package org.dsa11.team1.kumarketto.repository;

import org.dsa11.team1.kumarketto.domain.entity.Municipality;
import org.dsa11.team1.kumarketto.domain.entity.Prefecture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MunicipalityRepository extends JpaRepository<Municipality, Long> {
    List<Municipality> findByPrefecture_PrefId(Long prefecture);
}
