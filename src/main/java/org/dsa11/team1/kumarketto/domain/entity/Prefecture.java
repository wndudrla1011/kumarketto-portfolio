package org.dsa11.team1.kumarketto.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;

import java.util.*;

@DynamicInsert
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "prefecture")
public class Prefecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pref_id")
    private Long prefId; // 도도부현 ID

    @Column(name = "pref_name", length = 20, nullable = false)
    private String prefName; // 도도부현명

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rgn_id")
    private Region region; // 지역

    @OneToMany(mappedBy = "prefecture", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Municipality> municipalities = new HashSet<>();

    @Builder
    public Prefecture(String prefName, Region region) {
        this.prefName = prefName;
        this.region = region;
    }

    /* 연관 관계 메서드 */
    public void bindRegion(Region region) {
        this.region = region;
    }

    public void addMunicipality(Municipality municipality) {
        this.municipalities.add(municipality);
        municipality.bindPrefecture(this);
    }

}
