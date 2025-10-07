package org.dsa11.team1.kumarketto.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "store")
public class Store extends BaseTimeEntity {

    @Id
    @Column(name = "user_no")
    private Long id; // 회원 ID

    @OneToOne
    @MapsId // MemberEntity 의 PK를 @Id 에 매핑
    @JoinColumn(name = "user_no")
    private MemberEntity member; // 회원

    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // 소개글

    @Column(name = "profile_image", length = 255)
    private String profileImage; // 프로필 이미지

    @Builder
    public Store(MemberEntity member) {
        this.member = member;
    }

    /* 연관 관계 편의 메서드 */
    public void updateInfo(String description, String profileImage) {
        this.description = description;
        this.profileImage = profileImage;
    }

}
