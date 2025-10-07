package org.dsa11.team1.kumarketto.domain.enums;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IsPublic {
    PUBLIC("公開"), //공개
    PRIVATE("非公開"); //비공개

    private final String title;//비공개
}
