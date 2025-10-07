package org.dsa11.team1.kumarketto.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.dsa11.team1.kumarketto.domain.enums.ProductStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "product")
public class Product extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long pid; // 상품 ID

    @Column(name = "name", length = 100, nullable = false)
    private String name; // 상품명

    @Column(name = "price", nullable = false)
    private Integer price; // 상품가격

    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0; // 조회수

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status; //상품상태

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description; // 상품설명

    @Column(name = "image_url", length = 500)
    private String imageUrl; // 메인이미지 URL

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("imageSeq ASC")
    private List<ProductImage> images = new ArrayList<>(); // 상세이미지

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_no", referencedColumnName = "user_no")
    private MemberEntity member; // 회원 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcategory_id", referencedColumnName = "subcategory_id")
    @NotNull
    private SubCategory subCategory; // 하위카테고리ID

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductRegionEntity> productRegions = new ArrayList<>(); // 지역 목록

    @Builder
    public Product(String name, Integer price, Integer viewCount, ProductStatus status, String description) {
        this.name = name;
        this.price = price;
        this.viewCount = viewCount;
        this.status = status;
        this.description = description;
    }

    /* ======= 편의 메서드 ======= */
    public void addImage(ProductImage image) {
        images.add(image); // 상품이미지 목록에 이미지 추가
        image.bindProduct(this); // ProductImage <- Product 바인딩
    }

    public void removeImage(ProductImage image) {
        images.remove(image);
        image.unbindProduct();
    }

    public void bindMember(MemberEntity member) {
        this.member = member;
    }

    public void bindSubcategory(SubCategory subCategory) {
        this.subCategory = subCategory;
    }

    public void addProductRegion(ProductRegionEntity productRegion) {
        this.productRegions.add(productRegion);
        productRegion.setProduct(this);
    }

    public void removeProductRegion(ProductRegionEntity productRegion) {
        this.productRegions.remove(productRegion);
        productRegion.setProduct(null);
    }

    /* 대표 이미지 가져오기 */
    public ProductImage getMainImage() {
        return images.stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsMain()))
                .findFirst()
                .orElse(null);
    }

    /* 대표 이미지 변경하기 */
    public void changeMainImage(ProductImage newMain) {
        ProductImage oldMain = this.getMainImage();
        Integer oldMainSeq = oldMain.getImageSeq(); // 이전 메인 사진 seq
        Integer newMainSeq = newMain.getImageSeq(); // 새로 바꿀 메인 사진 seq

        images.forEach(img -> img.setMain(false)); //모든 사진을 메인에서 내림
        newMain.setMain(true); // 바꿀 새로운 사진을 메인으로 변경
        newMain.setImageSeq(1); //새로운 메인이미지 seq를 1로 변경
        oldMain.setImageSeq(newMainSeq); // 이전 메인 사진을 새로 바꿀 이미지 seq으로 변경


    }


    public List<ProductImage> getDetailImages() {
        return images.stream()
                .filter(img -> !Boolean.TRUE.equals(img.getIsMain()))
                .sorted(Comparator.comparingInt(ProductImage::getImageSeq))
                .collect(Collectors.toList());
    }

    @OneToMany(mappedBy = "product")
    private List<Transaction> transactions = new ArrayList<>();

}
