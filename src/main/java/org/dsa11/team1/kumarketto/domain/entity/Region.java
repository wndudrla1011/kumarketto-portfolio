package org.dsa11.team1.kumarketto.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "region")
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rgn_id")
    private Long rgnId; // 지역 ID

    @Column(name = "rgn_name")
    private String rgnName; // 지역명

    @OneToMany(mappedBy = "region", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Prefecture> prefectures = new HashSet<>(); // 하위 도도부현

    @Builder
    public Region(String rgnName) {
        this.rgnName = rgnName;
    }

    /* 연관 관계 메서드 */
    public void addPrefecture(Prefecture prefecture) {
        this.prefectures.add(prefecture);
        prefecture.bindRegion(this);
    }

}
