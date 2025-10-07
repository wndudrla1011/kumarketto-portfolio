package org.dsa11.team1.kumarketto.repository;

import org.dsa11.team1.kumarketto.domain.entity.MemberEntity;
import org.dsa11.team1.kumarketto.domain.entity.RecentSearch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface RecentSearchRepository extends JpaRepository<RecentSearch, Long> {

    // 특정 유저의 최근 검색어를 최신순으로 10개만 조회
    List<RecentSearch> findTop10ByMemberOrderBySearchAtDesc(MemberEntity member);

    // 특정 유저의 모든 최근 검색어 삭제
    void deleteAllByMember(MemberEntity member);

    // ID와 회원으로 특정 검색 기록 찾기
    Optional<RecentSearch> findByRecentSearchIdAndMember(Long recentSearchId, MemberEntity member);

    // 특정 유저의 최근 검색어가 10개를 넘을 시 삭제
    @Modifying
    @Transactional
    @Query(value = """
            DELETE rs FROM recent_search rs
            WHERE rs.recent_search_id IN (
                SELECT recent_search_id FROM (
                    SELECT 
                        recent_search_id,
                        ROW_NUMBER() OVER(PARTITION BY user_no ORDER BY search_at DESC) as rn
                    FROM recent_search
                ) ranked_rs
                WHERE ranked_rs.rn > 10
            )
            """, nativeQuery = true)
    void cleanupOldSearches();

}
