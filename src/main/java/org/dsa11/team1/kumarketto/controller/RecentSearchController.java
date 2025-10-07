package org.dsa11.team1.kumarketto.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsa11.team1.kumarketto.domain.dto.RecentSearchDTO;
import org.dsa11.team1.kumarketto.domain.entity.MemberEntity;
import org.dsa11.team1.kumarketto.repository.MemberRepository;
import org.dsa11.team1.kumarketto.security.AuthenticatedUser;
import org.dsa11.team1.kumarketto.service.RecentSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recent-searches")
public class RecentSearchController {

    private final RecentSearchService recentSearchService;
    private final MemberRepository memberRepository;

    /**
     * 최근 검색어 목록 조회
     * @param userDetails
     * @return 검색어 목록
     */
    @GetMapping
    public ResponseEntity<List<RecentSearchDTO>> getRecentSearches(@AuthenticationPrincipal AuthenticatedUser userDetails) {

        log.info("최근 검색어 목록 조회");

        // 로그인하지 않은 사용자는 빈 목록 반환
        if (userDetails == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        String userId = userDetails.getUsername();

        MemberEntity member = memberRepository.findByUserId(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Long userNo = member.getUserNo();

        List<RecentSearchDTO> searches = recentSearchService.getRecentKeywords(userNo);
        return ResponseEntity.ok(searches);

    }

    /**
     * 검색어 저장
     * @param payload
     * @param userDetails
     * @return 200 ok
     */
    @PostMapping
    public ResponseEntity<Void> addRecentSearch(@RequestBody Map<String, String> payload,
                                                @AuthenticationPrincipal AuthenticatedUser userDetails) {

        log.info("검색어 저장");

        if (userDetails != null) {
            String userId = userDetails.getUsername();
            MemberEntity member = memberRepository.findByUserId(userId)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            Long userNo = member.getUserNo();

            String keyword = payload.get("keyword");
            if (keyword != null && !keyword.isBlank()) {
                recentSearchService.addRecentKeyword(userNo, keyword.trim());
            }
        }

        return ResponseEntity.ok().build();

    }

    /**
     * 검색어 전체 삭제
     * @param userDetails
     * @return 200 ok
     */
    @DeleteMapping
    public ResponseEntity<Void> clearRecentSearches(@AuthenticationPrincipal AuthenticatedUser userDetails) {

        log.info("검색어 전체 삭제");

        if (userDetails != null) {
            String userId = userDetails.getUsername();
            MemberEntity member = memberRepository.findByUserId(userId)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            Long userNo = member.getUserNo();
            recentSearchService.deleteAllRecentKeywords(userNo);
        }

        return ResponseEntity.ok().build();

    }

    /**
     * 검색어 단건 삭제
     * @param userDetails       로그인 사용자
     * @param recentSearchId    선택된 검색어
     * @return                  200 ok
     */
    @DeleteMapping("/{recentSearchId}")
    public ResponseEntity<Void> deleteRecentSearch(@AuthenticationPrincipal AuthenticatedUser userDetails,
                                                   @PathVariable("recentSearchId") Long recentSearchId) {

        log.info("최근 검색어 ID: {} 를 삭제합니다.", recentSearchId);

        if (userDetails != null) {
            String userId = userDetails.getUsername();
            MemberEntity member = memberRepository.findByUserId(userId)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            Long userNo = member.getUserNo();
            recentSearchService.deleteOneRecentSearch(userNo, recentSearchId);
        }

        return ResponseEntity.ok().build();

    }

}
