package org.dsa11.team1.kumarketto.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.dsa11.team1.kumarketto.domain.entity.Region;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class RegionDTO {

    private Long rgnId; // 지역 ID

    private String rgnName; // 지역명

    private List<PrefectureDTO> prefectures; // 하위 도도부현

    public RegionDTO(Long rgnId, String rgnName) {
        this.rgnId = rgnId;
        this.rgnName = rgnName;
    }

    public RegionDTO(Region region) {
        this.rgnId = region.getRgnId();
        this.rgnName = region.getRgnName();
        this.prefectures = region.getPrefectures().stream()
                .map(PrefectureDTO::new)
                .collect(Collectors.toList());
    }

}
