package org.dsa11.team1.kumarketto.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetailDTO {

    // 1. 상품 기본 정보(ProductDTO에서 가져오기)
    private Long pid;
    private String name;
    private Integer price;
    private String status;
    private Integer viewCount;
    private String description;
    private LocalDateTime createDate;
    private LocalDateTime modifiedDate;
    private String imageUrl;
    private Long userNo;
    private List<MultipartFile> detailUploads;


    // 2. 상품 이미지 리스트(ProductImageDTO)
    private List<ProductImageDTO> images;

    // 3. 판매자 정보
    private String nickName;

    // 4. 별점
   private Double averageRating;

    // 찜 유무
    private Boolean wished;

    //수정,삭제버튼을 위해
    private Boolean owner;

    private String displayStatus;

}