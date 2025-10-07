package org.dsa11.team1.kumarketto.domain.dto;

import lombok.Getter;
import org.dsa11.team1.kumarketto.domain.entity.MemberEntity;

/**
 * 상점 정보 응답
 * Back -> Front
 */
@Getter
public class StoreResponseDTO {

    private final Long userNo; // 회원 ID

    private final String nickname; // 닉네임

    private final String userId; // 로그인 ID

    private final String description; // 소개글

    private final String profileImageUrl; // 프로필 이미지

    private final StoreStatsDTO stats; // 평점

    /**
     * 회원 정보를 기반으로 상점 개설
     * @param member    상점 주인
     */
    public StoreResponseDTO(MemberEntity member, StoreStatsDTO stats) {
        this.userNo = member.getUserNo();
        this.nickname = member.getNickname();
        this.userId = member.getUserId();
        this.stats = stats;

        // 회원 상점이 개설되지 않은 경우 대비
        if (member.getStore() != null) {
            this.description = member.getStore().getDescription();
            this.profileImageUrl = member.getStore().getProfileImage();
        } else {
            this.description = "紹介文がありません。";
            this.profileImageUrl = "デフォルト画像URL";
        }
    }
}
