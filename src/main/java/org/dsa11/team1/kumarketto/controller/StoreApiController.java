package org.dsa11.team1.kumarketto.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsa11.team1.kumarketto.domain.dto.ProductListDTO;
import org.dsa11.team1.kumarketto.domain.dto.StoreResponseDTO;
import org.dsa11.team1.kumarketto.domain.dto.StoreReviewDTO;
import org.dsa11.team1.kumarketto.domain.dto.StoreUpdateRequestDTO;
import org.dsa11.team1.kumarketto.domain.enums.ProductStatus;
import org.dsa11.team1.kumarketto.security.AuthenticatedUser;
import org.dsa11.team1.kumarketto.service.ProductService;
import org.dsa11.team1.kumarketto.service.ReviewService;
import org.dsa11.team1.kumarketto.service.StoreService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stores")
public class StoreApiController {

    private final StoreService storeService;
    private final ProductService productService;
    private final ReviewService reviewService;

    /**
     * 상점 정보 조회 API
     * @param userNo    상점 주인의 회원 ID
     * @return  상점 정보
     */
    @GetMapping("/{userNo}")
    public ResponseEntity<StoreResponseDTO> getStore(@PathVariable Long userNo) {

        log.info("상점 정보 조회 API");

        StoreResponseDTO storeInfo = storeService.getStoreInfo(userNo);
        return ResponseEntity.ok(storeInfo);

    }

    /**
     * 상점 정보 수정 API
     * @param userNo            상점 주인의 회원 ID
     * @param requestDTO        수정할 정보
     * @param authenticatedUser 현재 로그인된 사용자 정보
     * @return  수정된 상점 정보
     */
    @PutMapping("/{userNo}")
    public ResponseEntity<StoreResponseDTO> updateStore(
            @PathVariable Long userNo,
            @RequestBody StoreUpdateRequestDTO requestDTO,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {

        log.info("상점 정보 수정 API");

        // 로그인 상태 확인
        if (authenticatedUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        String currentUserId = authenticatedUser.getUsername();
        StoreResponseDTO updatedStoreInfo = storeService.updateStoreInfo(userNo, requestDTO, currentUserId);

        return ResponseEntity.ok(updatedStoreInfo);

    }

    /**
     * 상점 프로필 이미지 업로드 API
     * @param userNo 상점 주인의 ID
     * @param imageFile 업로드된 이미지 파일
     * @param authenticatedUser 현재 로그인된 사용자 정보 (권한 확인용)
     * @return 새로 업로드된 이미지의 URL 을 담은 JSON
     * @throws IOException 파일 처리 중 발생할 수 있는 예외
     */
    @PostMapping("/{userNo}/profile-image")
    public ResponseEntity<Map<String, String>> uploadProfileImage(
            @PathVariable Long userNo,
            @RequestParam("profileImageFile") MultipartFile imageFile,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) throws IOException {

        String currentUserId = authenticatedUser.getUsername();
        String newImageUrl = storeService.updateProfileImage(userNo, imageFile, currentUserId);

        // JSON 형태로 URL 반환
        return ResponseEntity.ok(Map.of("profileImageUrl", newImageUrl));

    }

    /**
     * 특정 상점의 상품 목록을 페이징하여 조회하는 API (무한 스크롤용)
     * @param userNo 상점 주인의 ID
     * @param pageable 페이징 정보 (page, size, sort)
     * @return 페이징된 상품 DTO 목록
     */
    @GetMapping("/{userNo}/products")
    public ResponseEntity<Page<ProductListDTO>> getStoreProducts(
            @PathVariable Long userNo,
            @RequestParam(required = false) ProductStatus status,
            @PageableDefault(size = 10, sort = "modifiedDate", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("상점의 상품 목록 조회");

        Page<ProductListDTO> productPage = productService.getProductsByStore(userNo, status, pageable);
        return ResponseEntity.ok(productPage);

    }

    /**
     * 특정 상점의 리뷰 목록을 페이징하여 조회하는 API (무한 스크롤용)
     * @param userNo 상점 주인의 ID
     * @param pageable 페이징 정보 (page, size, sort)
     * @return 페이징된 리뷰 DTO 목록
     */
    @GetMapping("/{userNo}/reviews")
    public ResponseEntity<Page<StoreReviewDTO>> getStoreReviews(
            @PathVariable Long userNo,
            @RequestParam(defaultValue = "SELLER") String role,
            @RequestParam(defaultValue = "false") boolean hasPhoto,
            @PageableDefault(size = 10, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<StoreReviewDTO> reviewPage = reviewService.getReviewsByStore(userNo, role, hasPhoto, pageable);
        return ResponseEntity.ok(reviewPage);

    }

}
