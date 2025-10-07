package org.dsa11.team1.kumarketto.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsa11.team1.kumarketto.domain.dto.RecentSearchDTO;
import org.dsa11.team1.kumarketto.domain.entity.MemberEntity;
import org.dsa11.team1.kumarketto.domain.entity.RecentSearch;
import org.dsa11.team1.kumarketto.repository.MemberRepository;
import org.dsa11.team1.kumarketto.repository.RecentSearchRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RecentSearchService {

    private final RecentSearchRepository recentSearchRepository;
    private final MemberRepository memberRepository;

    /**
     * 최근 검색어 조회
     * @param userNo
     * @return 최근 검색어 목록
     */
    @Transactional(readOnly = true)
    public List<RecentSearchDTO> getRecentKeywords(Long userNo) {
        MemberEntity member = memberRepository.findById(userNo)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        List<RecentSearch> searches = recentSearchRepository.findTop10ByMemberOrderBySearchAtDesc(member);

        return searches.stream()
                .map(search -> new RecentSearchDTO(search.getRecentSearchId(), search.getKeyword()))
                .collect(Collectors.toList());
    }

    /**
     * 최근 검색어 저장
     * @param userNo
     * @param keyword
     */
    public void addRecentKeyword(Long userNo, String keyword) {
        MemberEntity member = memberRepository.findById(userNo)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 새로운 검색어 저장
        RecentSearch recentSearch = RecentSearch.builder()
                .member(member)
                .keyword(keyword)
                .build();

        recentSearchRepository.save(recentSearch);
    }

    /**
     * 최근 검색어 전체 삭제
     * @param userNo
     */
    public void deleteAllRecentKeywords(Long userNo) {
        MemberEntity member = memberRepository.findById(userNo)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        recentSearchRepository.deleteAllByMember(member);
    }

    /**
     * 특정 회원의 특정 최근 검색어 삭제
     * @param userNo           회원 ID
     * @param recentSearchId   선택된 최근 검색어
     */
    public void deleteOneRecentSearch(Long userNo, Long recentSearchId) {
        MemberEntity member = memberRepository.findById(userNo)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        RecentSearch searchToDelete = recentSearchRepository.findByRecentSearchIdAndMember(recentSearchId, member)
                .orElseThrow(() -> new SecurityException("삭제 권한이 없는 최근 검색어입니다. ID:" + recentSearchId));

        recentSearchRepository.delete(searchToDelete);
        log.info("최근 검색어 ID '{}'를 삭제했습니다.", recentSearchId);
    }

    /**
     * 매일 새벽 4시에 실행
     * 각 사용자별로 10개가 넘는 오래된 최근 검색어 기록 삭제
     */
    @Scheduled(cron = "0 0 4 * * ?") // 매일 새벽 4시에 실행
    @Transactional
    public void cleanupOldRecentSearches() {
        log.info("오래된 최근 검색어 기록 정리 작업을 시작합니다...");

        try {
            recentSearchRepository.cleanupOldSearches();
            log.info("오래된 최근 검색어 기록 정리 작업이 성공적으로 완료되었습니다.");
        } catch (Exception e) {
            log.error("최근 검색어 기록 정리 작업 중 오류가 발생했습니다.", e);
        }
    }

}
