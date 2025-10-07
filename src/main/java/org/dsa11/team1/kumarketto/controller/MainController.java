package org.dsa11.team1.kumarketto.controller;

import jakarta.servlet.http.HttpServletRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsa11.team1.kumarketto.domain.dto.CategoryMainDTO;
import org.dsa11.team1.kumarketto.domain.dto.ProductListDTO;
import org.dsa11.team1.kumarketto.domain.dto.RegionDTO;
import org.dsa11.team1.kumarketto.service.CategoryService;
import org.dsa11.team1.kumarketto.service.LocationService;
import org.dsa11.team1.kumarketto.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MainController {

    private final ProductService productService;
    private final LocationService locationService;
    private final CategoryService categoryService;
    private final ObjectMapper objectMapper; // JSON 변환을 위해 주입

    /**
     * 메인 페이지 조회
     * @param model
     * @return 메인 페이지
     */
    @GetMapping("/")
    public String mainPage(Model model) throws JsonProcessingException {

        log.info("메인 페이지 스켈레톤 로딩");

        /* 지역 */
        List<RegionDTO> locations = locationService.getHierarchicalLocations();
        model.addAttribute("locations", locations); // Thymeleaf 가 첫 번째 드롭다운을 그릴 때 사용

        String locationsJson = objectMapper.writeValueAsString(locations);
        model.addAttribute("locationsJson", locationsJson);

        /* 카테고리 */
        List<CategoryMainDTO> categories = categoryService.getHierarchicalCategories();
        model.addAttribute("categories", categories);

        return "index";

    }

    /**
     * 상품 검색
     * @param model         모델
     * @param pageable      페이지 정보
     * @param searchWord    검색어
     * @param subCategoryId 서브 카테고리
     * @return 메인 페이지
     */
    @GetMapping("/search")
    public String products(Model model
            , @PageableDefault(sort = "modifiedDate", direction = Sort.Direction.DESC) Pageable pageable
            , @RequestParam(name = "searchWord", defaultValue = "") String searchWord
            , @RequestParam(name = "subCategoryId", required = false) Integer subCategoryId) {

        log.info("상품 검색어: {}", searchWord);

        Page<ProductListDTO> productPage = productService.getList(pageable, searchWord, subCategoryId);

        model.addAttribute("productPage", productPage); // 상품 목록 한 페이지
        model.addAttribute("searchWord", searchWord); // 검색어

        log.debug("전체 상품수 :{}, 전체 페이지수 :{}", productPage.getTotalElements(), productPage.getTotalPages());

        return "index";

    }

}
