package org.dsa11.team1.kumarketto.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.dsa11.team1.kumarketto.domain.entity.SubCategory;

@Getter
@NoArgsConstructor
public class SubCategoryMainDTO {

    private Integer id; // 서브카테고리 ID

    private String name; // 서브카테고리명

    public SubCategoryMainDTO(SubCategory subCategory) {
        this.id = subCategory.getId();
        this.name = subCategory.getName();
    }

}
