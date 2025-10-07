package org.dsa11.team1.kumarketto.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "recent_search")
public class RecentSearch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recentSearchId; // 최근 검색 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_no", nullable = false)
    private MemberEntity member; // 회원

    @Column(nullable = false, length = 100)
    private String keyword; // 키워드

    @Column(nullable = false)
    private LocalDateTime searchAt; // 검색 시간

    @Builder
    public RecentSearch(MemberEntity member, String keyword) {
        this.member = member;
        this.keyword = keyword;
        this.searchAt = LocalDateTime.now();
    }

}
