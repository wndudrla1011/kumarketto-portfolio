package org.dsa11.team1.kumarketto.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.dsa11.team1.kumarketto.domain.entity.Prefecture;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class PrefectureDTO {

    private Long prefId; // 도도부현 ID

    private String prefName; // 도도부현명

    private List<MunicipalityDTO> municipalities; // 하위 시구

    public PrefectureDTO(Prefecture prefecture) {
        this.prefId = prefecture.getPrefId();
        this.prefName = prefecture.getPrefName();
        this.municipalities = prefecture.getMunicipalities().stream()
                .map(MunicipalityDTO::new)
                .collect(Collectors.toList());
    }

}
