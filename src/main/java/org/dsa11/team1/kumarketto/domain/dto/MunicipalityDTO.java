package org.dsa11.team1.kumarketto.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.dsa11.team1.kumarketto.domain.entity.Municipality;

@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class MunicipalityDTO {

    private Long mId; // 시구 ID
    private String muniName; // 시구명

    public MunicipalityDTO(Municipality municipality) {
        this.mId = municipality.getMId();
        this.muniName = municipality.getMuniName();
    }

}
