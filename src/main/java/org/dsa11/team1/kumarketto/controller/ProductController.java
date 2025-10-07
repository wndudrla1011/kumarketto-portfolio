package org.dsa11.team1.kumarketto.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsa11.team1.kumarketto.domain.dto.*;
import org.dsa11.team1.kumarketto.domain.entity.MemberEntity;
import org.dsa11.team1.kumarketto.repository.MemberRepository;
import org.dsa11.team1.kumarketto.security.AuthenticatedUser;
import org.dsa11.team1.kumarketto.service.ProductService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;


@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("product")
public class ProductController {
    private final ProductService productService;
    private final MemberRepository memberRepository;

    /**
     * 글 쓰기 폼으로 이동
     */
    @GetMapping("write")
    public String write(Model model, @AuthenticationPrincipal AuthenticatedUser user) {
        MemberEntity member = memberRepository.findByUserId(user.getUsername()).get();
        model.addAttribute("userNo", member.getUserNo());

        model.addAttribute("product", new ProductDTO());

        List<CategoriesDTO> categories = productService.getAllCategories();
        model.addAttribute("categories", categories);

        List<RegionDTO> regions = productService.getAllRegions();
        model.addAttribute("regions", regions);

        return "/products/writeForm";
    }
    /**
     * 글 저장
     */
    @PostMapping("write")
    public String write(
            Model model,
            @ModelAttribute("product") @Valid ProductDTO productDTO,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal AuthenticatedUser user

    ) {
        // 유효성
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errors", bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("product", productDTO); // 입력값 유지시켜줌
            return "redirect:/product/write";
        }

        //상세이미지 최대 2장 제한
        if (productDTO.getDetailUploads() != null && productDTO.getDetailUploads().size() > 2) {
            bindingResult.rejectValue("detailUploads", "size.error", "상세 이미지는 최대 2장까지만 업로드 가능합니다.");
            redirectAttributes.addFlashAttribute("errors", bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("product", productDTO);
            return "redirect:/product/write";
        }

        MemberEntity member = memberRepository.findByUserId(user.getUsername()).get();
        productDTO.setUserNo(member.getUserNo());

        try {
            Long savedProductId = productService.write(
                    productDTO,
                    productDTO.getMainUpload(),
                    productDTO.getDetailUploads());
            return "redirect:/product/detail/" + savedProductId;
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errors", List.of("상품 등록 중 오류가 발생."));
            return "redirect:/product/write";
        }
    }

    /**
     * 수정폼 이동
     */
    @GetMapping("/update/{productId}")
    public String update(@PathVariable("productId") Long productId,
                         @AuthenticationPrincipal AuthenticatedUser user,
                         Model model) {
        try {
            // 수정: ProductService.getProduct()이 모든 필요한 데이터를 DTO에 담아주므로, 그대로 모델에 추가
            ProductDTO productDTO = productService.getProduct(productId);
            MemberEntity member = memberRepository.findByUserId(user.getUsername()).get();

            // 권한 체크
            if (!member.getUserNo().equals(productDTO.getUserNo())) {
                // 실제 서비스에서는 에러 페이지로 보내거나 권한 없음 메시지를 보여주는 것이 좋습니다.
                throw new RuntimeException("게시글 작성자가 아닙니다. 수정 권한이 없습니다.");
            }
            if (!"NEW".equals(productDTO.getStatus())) {
                throw new RuntimeException("거래중, 거래완료일땐 수정 불가능합니다.");
            }

            // productDTO 하나만 모델에 담아도 View에서 필요한 모든 데이터(카테고리, 지역 목록 포함)를 사용할 수 있습니다.
            model.addAttribute("product", productDTO);

            return "/products/updateForm";

        } catch (Exception e) {
            log.error("수정 폼 로딩 중 에러 발생: {}", e.getMessage());
            // 에러 발생 시 상세 페이지로 리다이렉트
            return "redirect:/product/detail/" + productId;
        }
    }

    /**
     * 판매글 수정
     * @param productDTO            판매글 데이터
     * @param bindingResult         오류 검증
     * @param redirectAttributes    리다이렉트
     * @return  상세 페이지
     */
    @PostMapping("update")
    public String update(
            @ModelAttribute @Valid ProductDTO productDTO,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        // 유효성 검사
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errors", bindingResult.getAllErrors());
            redirectAttributes.addFlashAttribute("product", productDTO); // 입력값 유지
            return "redirect:/product/update/" + productDTO.getPid();
        }

        log.info("수정 요청 DTO: {}", productDTO);
        log.info("municipalityId: {}", productDTO.getMunicipalityId());
        log.info("newMainImageId: {}", productDTO.getNewMainImageId()); // 로그 추가

        try {
            productService.update(
                    productDTO,
                    productDTO.getMainUpload(),
                    productDTO.getDetailUploads()
            );
            log.info("수정 완료: pid={}", productDTO.getPid());
            return "redirect:/product/detail/" + productDTO.getPid();
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "상품 수정 중 오류가 발생했습니다.");
            return "redirect:/product/update/" + productDTO.getPid();
        }
    }


    // 카테고리 선택 시 서브카테고리 조회
    @GetMapping("subcategories")
    @ResponseBody
    public List<SubCategoriesDTO> getSubCategories(@RequestParam("categoryId") Integer categoryId) {
        return productService.getSubCategoriesByCategories(categoryId);
    }

    // Region 선택 시 Prefecture 리스트 조회
    @GetMapping("prefectures")
    @ResponseBody
    public List<PrefectureDTO> getPrefectures(@RequestParam("regionId") Long regionId) {
        return productService.getPrefecturesByRegion(regionId);
    }

    // Prefecture 선택 시 Municipality 리스트 조회
    @GetMapping("municipalities")
    @ResponseBody
    public List<MunicipalityDTO> getMunicipalities(@RequestParam("prefectureId") Long prefectureId) {
        return productService.getMunicipalitiesByPrefecture(prefectureId);
    }

    /**
     * 삭제기능
     *
     * @return
     */
    @GetMapping("/delete")
    public String delete(@RequestParam("pid") Long productId,
                         @AuthenticationPrincipal AuthenticatedUser user, Model model) {

        try {
            productService.delete(productId, user.getUsername());
        } catch (RuntimeException e) {
        }
        return "redirect:/"; //삭제하면 메인으로 이동

    }


}