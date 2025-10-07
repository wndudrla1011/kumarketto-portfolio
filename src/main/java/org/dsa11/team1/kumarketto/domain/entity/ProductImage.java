package org.dsa11.team1.kumarketto.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "product_image")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long id; // 이미지 ID

    @Column(name = "image_url", length = 500)
    private String imageUrl; // 이미지 URL

    @Column(name = "is_main", nullable = false)
    private Boolean isMain; // 메인여부

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product; // 상품

    @Column(name = "image_seq", nullable = false)
    private Integer imageSeq; // 이미지순서

    @Builder
    public ProductImage(String imageUrl, Boolean isMain, Product product, Integer imageSeq) {
        this.imageUrl = imageUrl;
        this.isMain = isMain;
        this.product = product;
        this.imageSeq = imageSeq;
    }

    /* 편의 메서드 */
    public void bindProduct(Product product) {
        this.product = product;
    }

    public void unbindProduct() {
        this.product = null;
    }

    public void setMain(Boolean main) {
        isMain = main;
    }

    public void bindImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getSequence() {
        return imageSeq;
    }

    public void setSequence(int sequence) {
        this.imageSeq = sequence;
    }

    public void setImageSeq(Integer imageSeq) {
        this.imageSeq = imageSeq;
    }

}
