package org.dsa11.team1.kumarketto.domain.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상점 정보 수정
 * Front -> Back
 */
@Getter
@NoArgsConstructor
public class StoreUpdateRequestDTO {

    private String description; // 소개글

    private String profileImage; // 프로필 이미지

}
