package org.dsa11.team1.kumarketto.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dsa11.team1.kumarketto.domain.entity.ProductImage;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Long pid;

    @NotBlank(message="商品名を入力してください")
    @Size(max=50, message="商品名は最大100文字まで入力可能です")
    private String name;

    @NotNull(message = "価格を入力してください")
    @Min(value = 0, message = "価格は0円以上である必要があります")
    private Integer price;
    private String status;
    private Integer viewCount;

    @NotBlank(message = "説明を入力してください")
    @Size(max = 1000, message = "説明は最大1000字まで可能です")
    private String description;
    private LocalDateTime createDate;
    private LocalDateTime modifiedDate;
    private String imageUrl;
    private Long mainImageId; // 수정: 메인 이미지 ID 필드 추가
    private Integer categoryId; // 카테고리id
    private Integer subcategoryId; //하위카테고리ID
    private Long userNo;
    private MultipartFile mainUpload;
    private List<MultipartFile> detailUploads;
    private List<ProductImageDTO> images;
    private ProductRegionDTO productRegionDTO;
    private String regionName;
    @NotNull(message = "시정촌을 선택해야 합니다.")
    private Long municipalityId;

    @JsonIgnore
    private List<RegionDTO> regions;
    @JsonIgnore
    private List<SubCategoriesDTO> subCategories;
    @JsonIgnore
    private List<PrefectureDTO> prefectures;
    @JsonIgnore
    private List<MunicipalityDTO> municipalities;

    private Long selectedRegionId;
    private Long selectedPrefectureId;
    private String userName;
    private List<CategoriesDTO> categories;

    private List<Long> deletedImageIds;
    private Long newMainImageId;
    private List<Long> existingDetailImageIds; // 기존유지 할 이미지 id목록

    public List<ProductImageDTO> getDetailImages() {
        if (this.images == null) {
            return Collections.emptyList();
        }
        return this.images.stream()
                .filter(img -> img.getImageSeq() != null && img.getImageSeq() > 1)
                .sorted(Comparator.comparing(ProductImageDTO::getImageSeq))
                .collect(Collectors.toList());
    }
}
