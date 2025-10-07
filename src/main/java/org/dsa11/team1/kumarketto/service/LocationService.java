package org.dsa11.team1.kumarketto.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsa11.team1.kumarketto.domain.dto.RegionDTO;
import org.dsa11.team1.kumarketto.repository.RegionRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationService {

    private final RegionRepository regionRepository;

    /**
     * 전체 지역 조회
     * @return 전체 지역 목록
     */
    @Cacheable("locations")
    public List<RegionDTO> getHierarchicalLocations() {
        log.info("DB 에서 지역 정보를 조회합니다...");

        return regionRepository.findAllWithPrefecturesAndMunicipalities().stream()
                .map(RegionDTO::new)
                .collect(Collectors.toList());
    }

}
