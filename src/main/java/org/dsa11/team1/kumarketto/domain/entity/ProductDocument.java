package org.dsa11.team1.kumarketto.domain.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Builder
@Data
@Document(indexName = "products", createIndex = false) // Elasticsearch 에 인덱스 이름
public class ProductDocument {

    @Id
    private Long pid; // 상품 ID

    @Field(type = FieldType.Text, analyzer = "kuromoji")
    private String name; // 상품명

    @Field(type = FieldType.Integer)
    private Integer price; // 가격

    @Field(type = FieldType.Integer)
    private Integer viewCount; // 조회수

    @Field(type = FieldType.Long)
    private Long likeCount; // 찜 수

    @Field(type = FieldType.Keyword)
    private String status; // 상품 상태

    @Field(type = FieldType.Keyword, index = false) // 검색 대상에서 제외
    private String imageUrl; // 상품 이미지

    @Field(type = FieldType.Date, format = {},
            pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'||yyyy-MM-dd HH:mm:ss||yyyy-MM-dd")
    private LocalDateTime modifiedDate; // 수정 날짜

    @Field(type = FieldType.Long)
    private List<Long> muniIds; // 지역 목록

    @Field(type = FieldType.Long, name = "subcategory_id")
    private Integer subCategoryId; // 서브 카테고리

    public static ProductDocument fromProduct(Product product, Long likeCount) {
        List<Long> municipalityIds  = product.getProductRegions() != null
                ? product.getProductRegions().stream()
                    .map(pr -> pr.getMunicipality().getMId())
                    .toList() : List.of();

        return ProductDocument.builder()
                .pid(product.getPid())
                .name(product.getName())
                .price(product.getPrice())
                .viewCount(product.getViewCount())
                .likeCount(likeCount)
                .status(product.getStatus() != null ? product.getStatus().name() : null)
                .imageUrl(product.getImageUrl())
                .modifiedDate(product.getModifiedDate())
                .muniIds(municipalityIds)
                .subCategoryId(product.getSubCategory() != null ? product.getSubCategory().getId() : null)
                .build();
    }

}
