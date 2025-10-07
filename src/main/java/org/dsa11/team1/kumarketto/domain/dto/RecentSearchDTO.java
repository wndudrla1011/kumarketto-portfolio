package org.dsa11.team1.kumarketto.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 클릭된 최근 검색어
 * Front -> Back
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecentSearchDTO {

    private Long id; // 최근 검색어 ID

    private String keyword; // 검색어

}
