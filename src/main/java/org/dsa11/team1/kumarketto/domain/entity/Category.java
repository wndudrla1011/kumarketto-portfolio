package org.dsa11.team1.kumarketto.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "categories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Integer id; // 카테고리 ID

    @Column(name = "category_name", nullable = false, length = 20)
    private String name; // 카테고리명

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubCategory> subCategories = new ArrayList<>(); // 서브 카테고리

    @Builder
    public Category(String name) {
        this.name = name;
    }

    /* ======= 편의 메서드 ======= */
    public void addSubCategory(SubCategory sub) {
        subCategories.add(sub);
        sub.bindCategory(this);
    }

    public void removeSubCategory(SubCategory sub) {
        subCategories.remove(sub);
        sub.bindCategory(null);
    }

}
