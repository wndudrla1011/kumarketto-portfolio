package org.dsa11.team1.kumarketto.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.dsa11.team1.kumarketto.domain.dto.ReviewStatsDTO;
import org.dsa11.team1.kumarketto.domain.dto.StoreResponseDTO;
import org.dsa11.team1.kumarketto.domain.dto.StoreStatsDTO;
import org.dsa11.team1.kumarketto.domain.dto.StoreUpdateRequestDTO;
import org.dsa11.team1.kumarketto.domain.entity.MemberEntity;
import org.dsa11.team1.kumarketto.domain.enums.ProductStatus;
import org.dsa11.team1.kumarketto.domain.entity.Store;
import org.dsa11.team1.kumarketto.repository.MemberRepository;
import org.dsa11.team1.kumarketto.repository.ProductRepository;
import org.dsa11.team1.kumarketto.repository.TradingReviewRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final TradingReviewRepository tradingReviewRepository;
    private final S3Service s3Service;

    /**
     * 특정 회원의 상점 정보 조회
     * @param userNo    상점 주인의 회원 ID
     * @return  상점 정보 DTO
     */
    public StoreResponseDTO getStoreInfo(Long userNo) {

        MemberEntity member = memberRepository.findById(userNo)
                .orElseThrow(() -> new EntityNotFoundException("해당 회원을 찾을 수 없습니다. ID: " + userNo));

        // 상품 관련 통계 조회
        long totalProductCount = productRepository.countByMember_UserNo(userNo);
        long transactionCount = productRepository.countByMember_UserNoAndStatus(userNo, ProductStatus.SOLDOUT);

        // 리뷰 관련 통계 조회
        ReviewStatsDTO reviewStats = tradingReviewRepository.getReviewStatsBySellerUserNo(userNo);

        // 통계 DTO 생성
        StoreStatsDTO statsDTO = StoreStatsDTO.builder()
                .totalProductCount(totalProductCount)
                .transactionCount(transactionCount)
                .reviewCount(reviewStats.getReviewCount())
                .averageRating(reviewStats.getAverageScore())
                .build();

        return new StoreResponseDTO(member, statsDTO);

    }

    /**
     * 특정 회원의 상점 정보 수정
     * @param userNo        상점 주인 회원 ID
     * @param requestDTO    수정한 상점 데이터
     * @param currentUserId 현재 로그인한 사용자의 회원 ID
     * @return  수정된 상점 정보 DTO
     */
    @Transactional
    public StoreResponseDTO updateStoreInfo(Long userNo, StoreUpdateRequestDTO requestDTO, String currentUserId) {

        MemberEntity member = memberRepository.findById(userNo)
                .orElseThrow(() -> new EntityNotFoundException("해당 회원을 찾을 수 없습니다. ID: " + userNo));

        // 상점 주인 여부 확인
        if (!member.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("상점 정보를 수정할 권한이 없습니다.");
        }

        // 회원의 상점 정보 조회
        Store store = member.getStore();
        if (store == null) {
            store = Store.builder().member(member).build();
            member.bindStore(store); // 재 바인딩
        }

        // 상점 정보 갱신
        store.updateInfo(requestDTO.getDescription(), requestDTO.getProfileImage());

        return getStoreInfo(userNo);

    }

    /**
     * 상점 프로필 이미지 업로드 + URL 업데이트
     * @param userNo 상점 주인의 ID
     * @param imageFile 업로드된 이미지 파일
     * @param currentUserId 현재 로그인된 사용자의 ID (권한 확인용)
     * @return 새로 업로드된 이미지의 URL
     */
    @Transactional
    public String updateProfileImage(Long userNo, MultipartFile imageFile, String currentUserId) throws IOException {

        // 회원 조회
        MemberEntity member = memberRepository.findById(userNo)
                .orElseThrow(() -> new EntityNotFoundException("해당 회원을 찾을 수 없습니다. ID: " + userNo));

        // 권한 확인
        if (!member.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("프로필 이미지를 수정할 권한이 없습니다.");
        }

        // 상점 수정
        Store store = member.getStore();
        if (store == null) {
            store = Store.builder().member(member).build();
            member.setStore(store);
        }

        // 기존 이미지가 있다면 S3에서 삭제
        if (store.getProfileImage() != null && !store.getProfileImage().isBlank()) {
            s3Service.deleteFile(store.getProfileImage());
        }

        // 새 이미지를 "profiles/" 디렉터리에 업로드
        String imageUrl = s3Service.uploadFile(imageFile, "profiles/");

        // Store 엔티티에 새 이미지 URL 저장
        store.updateInfo(store.getDescription(), imageUrl);

        return imageUrl;

    }

}
