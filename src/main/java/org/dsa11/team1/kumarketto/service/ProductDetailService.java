package org.dsa11.team1.kumarketto.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsa11.team1.kumarketto.domain.dto.ProductDetailDTO;
import org.dsa11.team1.kumarketto.domain.dto.ProductImageDTO;
import org.dsa11.team1.kumarketto.domain.dto.ReviewStatsDTO;
import org.dsa11.team1.kumarketto.domain.entity.*;
import org.dsa11.team1.kumarketto.domain.enums.ProductStatus;
import org.dsa11.team1.kumarketto.repository.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class    ProductDetailService {
    private final ProductRepository productRepository;
    private final WishListRepository wishListRepository;
    private final MemberRepository memberRepository;
    private final TradingReviewRepository tradingReviewRepository;
    private final ProductElasticsearchRepository productElasticsearchRepository;

    public ProductDetailDTO getProductDetail(Long productId, String userNo) {

        // 상품 엔티티 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("해당 상품이 없습니다. ID=" + productId));

        // 리뷰 통계 조회
        ReviewStatsDTO reviewStats = tradingReviewRepository.getReviewStatsBySellerUserNo(product.getMember().getUserNo());


        // 이미지 리스트 변환
        List<ProductImageDTO> images = product.getImages().stream()
                .map(img -> ProductImageDTO.builder()
                        .imageId(img.getId())
                        .imageUrl(img.getImageUrl())
                        .isMain(img.getIsMain())
                        .imageSeq(img.getImageSeq())
                        .build())
                .sorted(Comparator.comparing(ProductImageDTO::getImageSeq))
                .collect(Collectors.toList());

        // DTO 생성
        ProductDetailDTO dto = ProductDetailDTO.builder()
                .pid(product.getPid())
                .name(product.getName())
                .price(product.getPrice())
                .status(product.getStatus().name())
                .viewCount(product.getViewCount())
                .description(product.getDescription())
                .imageUrl(product.getImageUrl())
                .userNo(product.getMember().getUserNo())
                .nickName(product.getMember().getNickname())
                .images(images)
                .averageRating(reviewStats.getAverageScore())
                .build();

        // 상태 이름 바꾸기
        String displayStatus;
        switch (product.getStatus().name()) {
            case "NEW":
                displayStatus = ProductStatus.NEW.getTitle();
                break;
            case "RESERVED":
                displayStatus = ProductStatus.RESERVED.getTitle();
                break;
            case "SOLDOUT":
                displayStatus = ProductStatus.SOLDOUT.getTitle();
                break;
            case "REPORTED":
                displayStatus = ProductStatus.REPORTED.getTitle();
                break;
            default:
                displayStatus = "error";
        }

        dto.setDisplayStatus(displayStatus);

        // 4. 로그인 사용자가 있을 때 찜 여부 확인
        if (userNo != null) {
            MemberEntity member = memberRepository.findByUserId(userNo)
                    .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다. userId=" + userNo));

            // 찜 여부 확인
            boolean wished = wishListRepository.findByMemberAndProduct(member, product).isPresent();
            dto.setWished(wished);

            // 상품 주인의 userNo와 현재 로그인한 사용자의 userNo를 비교
            boolean owner = member.getUserNo().equals(product.getMember().getUserNo());
            dto.setOwner(owner);
        } else {
            dto.setOwner(false);
            dto.setWished(false);
        }

        return dto;
    }

    private ProductImageDTO convertToImageDTO(ProductImage image) {
        return ProductImageDTO.builder()
                .imageId(image.getId())
                .imageUrl(image.getImageUrl())
                .isMain(image.getIsMain())
                .imageSeq(image.getImageSeq())
                .build();
    }

    /**
     * 찜 목록 추가
     * @param userNo        회원 ID
     * @param productId     상품 ID
     * @return 찜 수
     */
    @Transactional(readOnly = false)
    @CacheEvict(value = "mainProducts", allEntries = true)
    public boolean wish(Long userNo, Long productId) {
        MemberEntity member = memberRepository.findById(userNo)
                .orElseThrow(() -> new RuntimeException("회원 정보 없음"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("상품 정보 없음"));

        // 현재 찜 목록에 해당 상품이 있는지 확인
        Optional<WishList> existing = wishListRepository.findByMemberAndProduct(member, product);
        boolean isWished;

        if (existing.isPresent()) { // 찜 존재 -> 취소
            wishListRepository.delete(existing.get());
            isWished = false;
        } else { // 찜 없음 -> 추가
            wishListRepository.save(new WishList(member, product));
            isWished = true;
        }

        updateLikeCountInES(productId);

        return isWished;

    }

    /**
     * 조회수 증가
     * @param productId
     */
    @Transactional
    public void incrementViewCount(Long productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 없습니다. ID=" + productId));
        product.setViewCount(product.getViewCount() + 1);

    }

    /**
     * [비동기] Elasticsearch의 찜 개수를 업데이트
     * @param productId 상품 ID
     */
    @Async
    @Transactional(readOnly = true)
    public void updateLikeCountInES(Long productId) {

        // 상품 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("상품을 찾을 수 없습니다: " + productId));

        /* 수정된 찜 현황을 ES에 반영 */
        long totalLikes = wishListRepository.countByProduct(product);

        ProductDocument productDocument = productElasticsearchRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Elasticsearch에서 상품을 찾을 수 없습니다: " + productId));

        productDocument.setLikeCount(totalLikes);

        productElasticsearchRepository.save(productDocument);
        log.info("[Async] Elasticsearch 상품 ID {}의 찜 개수를 {}로 업데이트했습니다.", productId, totalLikes);

    }

}
