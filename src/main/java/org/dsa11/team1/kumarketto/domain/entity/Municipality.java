package org.dsa11.team1.kumarketto.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;

@DynamicInsert
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "municipality")
public class Municipality {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "muni_id")
    private Long mId; // 시구 ID

    @Column(name = "muni_name")
    private String muniName; // 시구명

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pref_id")
    private Prefecture prefecture; // 도도부현

    @Builder
    public Municipality(Long mId, String muniName, Prefecture prefecture) {
        this.mId = mId;
        this.muniName = muniName;
        this.prefecture = prefecture;
    }

    /* 연관 관계 메서드 */
    public void bindPrefecture(Prefecture prefecture) {
        this.prefecture = prefecture;
    }

}
