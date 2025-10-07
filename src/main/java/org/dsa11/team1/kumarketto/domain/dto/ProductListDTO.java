package org.dsa11.team1.kumarketto.domain.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.dsa11.team1.kumarketto.domain.enums.ProductStatus;

/**
 * 검색 결과용 상품 목록
 */
@Getter
@NoArgsConstructor
public class ProductListDTO {

    private Long pid; // 상품 ID
    private String name; // 상품명
    private Integer price; // 상품가격
    private Integer viewCount; // 조회수
    private ProductStatus status; // 상품상태
    private String imageUrl; // 메인이미지 URL
    private Long likeCount; // 찜 수

    @Builder
    public ProductListDTO(Long pid, String name, Integer price, Integer viewCount, ProductStatus status, String imageUrl, Long likeCount) {
        this.pid = pid;
        this.name = name;
        this.price = price;
        this.viewCount = viewCount;
        this.status = status;
        this.imageUrl = imageUrl;
        this.likeCount = likeCount;
    }

}
