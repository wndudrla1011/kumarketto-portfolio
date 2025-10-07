package org.dsa11.team1.kumarketto.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoriesDTO {
    private Integer categoryId;
    private String categoryName;
}
