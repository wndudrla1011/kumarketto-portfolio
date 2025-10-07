package org.dsa11.team1.kumarketto.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "sub_categories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subcategory_id",nullable = false)
    private Integer id; // 서브카테고리 ID

    @Column(name = "subcategory_name", nullable = false, length = 20)
    private String name; // 서브카테고리명

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false, referencedColumnName = "category_id")
    private Category category; // 카테고리

    @OneToMany(mappedBy = "subCategory")
    private List<Product> products = new ArrayList<>(); // 상품

    @Builder
    public SubCategory(String name, Category category) {
        this.name = name;
        this.category = category;
    }

    /* ======= 편의 메서드 ======= */
    public void bindCategory(Category category) {
        this.category = category;
    }

}
