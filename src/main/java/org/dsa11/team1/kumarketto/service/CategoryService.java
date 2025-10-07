package org.dsa11.team1.kumarketto.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsa11.team1.kumarketto.domain.dto.CategoryMainDTO;
import org.dsa11.team1.kumarketto.repository.CategoryRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * 전체 카테고리 조회
     * @return 전체 카테고리 목록
     */
    @Cacheable("categories")
    public List<CategoryMainDTO> getHierarchicalCategories() {
        log.info("DB 에서 카테고리 정보를 조회합니다...");

        return categoryRepository.findAllWithSubCategories().stream()
                .map(CategoryMainDTO::new)
                .collect(Collectors.toList());
    }

}
