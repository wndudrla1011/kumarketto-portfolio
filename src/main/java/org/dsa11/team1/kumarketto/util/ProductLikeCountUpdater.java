package org.dsa11.team1.kumarketto.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsa11.team1.kumarketto.domain.entity.ProductDocument;
import org.dsa11.team1.kumarketto.repository.ProductElasticsearchRepository;
import org.dsa11.team1.kumarketto.repository.ProductRepository;
import org.dsa11.team1.kumarketto.repository.WishListRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductLikeCountUpdater {

    private final ProductRepository productRepository;
    private final WishListRepository wishListRepository;
    private final ProductElasticsearchRepository productElasticsearchRepository;

    @Scheduled(fixedRate = 10 * 60 * 1000) // 10min 마다
    @Transactional
    public void updateLikeCounts() {

        log.info("======= 상품 찜 수 업데이트 작업을 시작합니다 =======");

        // 모든 상품 ID 조회
        List<Long> allProductIds = productRepository.findAllIds();

        if (allProductIds.isEmpty()) {
            log.info("업데이트할 상품이 없습니다.");
            return;
        }

        // 상품 ID 별 찜 수 계산
        List<Object[]> likeCounts = wishListRepository.countLikesByProductIds(allProductIds);
        Map<Long, Long> likeCountMap = likeCounts.stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        // Elasticsearch 의 ProductDocument 의 찜 수 업데이트
        Iterable<ProductDocument> documents = productElasticsearchRepository.findAllById(allProductIds);
        List<ProductDocument> updatedDocuments = new ArrayList<>();

        documents.forEach(doc -> {
            Long likeCount = likeCountMap.getOrDefault(doc.getPid(), 0L);
            if (!likeCount.equals(doc.getLikeCount())) { // 찜 수가 변경된 경우
                doc.setLikeCount(likeCount); // 찜 수 업데이트
                updatedDocuments.add(doc);
            }
        });

        // 변경된 Document 만 Elasticsearch 에 다시 저장
        if (!updatedDocuments.isEmpty()) {
            productElasticsearchRepository.saveAll(updatedDocuments);
            log.info("{}개의 상품에 대한 찜 수를 업데이트했습니다.", updatedDocuments.size());
        } else {
            log.info("찜 수 변경 사항이 없습니다.");
        }

    }

}
