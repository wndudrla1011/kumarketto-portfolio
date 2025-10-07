package org.dsa11.team1.kumarketto.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsa11.team1.kumarketto.domain.dto.ProductListDTO;
import org.dsa11.team1.kumarketto.domain.entity.ProductDocument;
import org.dsa11.team1.kumarketto.domain.enums.ProductStatus;
import org.dsa11.team1.kumarketto.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductRestController {

    private final ProductService productService;

    /**
     * 메인 페이지 상품 목록 (JSON)
     *
     * @param pageable 페이징 정보 (page, size, sort)
     * @return 페이징된 상품 DTO 목록
     */
    @GetMapping("/products")
    public ResponseEntity<Page<ProductListDTO>> getMainProducts(
            @PageableDefault(size = 15, sort = "modifiedDate", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("메인 페이지 상품 목록 조회 (ES 가중치 적용)");

        Page<ProductDocument> productDocuments = productService.getMainPageProducts(pageable);

        Page<ProductListDTO> productPage = productDocuments.map(doc -> ProductListDTO.builder()
                .pid(doc.getPid())
                .name(doc.getName())
                .price(doc.getPrice())
                .viewCount(doc.getViewCount())
                .status(ProductStatus.valueOf(doc.getStatus()))
                .imageUrl(doc.getImageUrl())
                .likeCount(doc.getLikeCount())
                .build());

        return ResponseEntity.ok(productPage);

    }

    /**
     * 필터 검색
     * @param pageable
     * @param muniIds
     * @param subCategoryId
     * @param maxPrice
     * @param minPrice
     * @param keyword
     * @return 검색 결과 상품 목록
     */
    @GetMapping("/products/filter")
    public ResponseEntity<Page<ProductListDTO>> getFilteredProducts(
            @PageableDefault(sort = "modifiedDate", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) List<Long> muniIds,
            @RequestParam(required = false) Integer subCategoryId,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortDirection) {

        log.info("필터링이 적용된 검색 결과 상품 조회");

        // 정렬 객체 생성
        Sort sort = pageable.getSort();
        if (sortField != null && !sortField.isEmpty()) {
            Sort.Direction direction = Sort.Direction.DESC; // Default DESC
            if (sortDirection != null && sortDirection.equalsIgnoreCase("asc")) {
                direction = Sort.Direction.ASC;
            }
            sort = Sort.by(direction, sortField);
        }

        // 페이지 객체 생성
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        // 필터 검색
        Page<ProductListDTO> productPage =  productService.getFilteredList(sortedPageable, muniIds, subCategoryId, maxPrice, minPrice, keyword);

        return ResponseEntity.ok(productPage);

    }

}
