package org.dsa11.team1.kumarketto.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.dsa11.team1.kumarketto.domain.entity.Category;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
public class CategoryMainDTO {

    private Integer id; // 카테고리 ID

    private String name; // 카테고리명

    private List<SubCategoryMainDTO> subCategories; // 서브 카테고리

    public CategoryMainDTO(Category category) {
        this.id = category.getId();
        this.name = category.getName();
        this.subCategories = category.getSubCategories().stream()
                .map(SubCategoryMainDTO::new)
                .collect(Collectors.toList());
    }

}
