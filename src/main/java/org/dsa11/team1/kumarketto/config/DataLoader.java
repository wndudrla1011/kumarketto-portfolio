package org.dsa11.team1.kumarketto.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsa11.team1.kumarketto.domain.entity.Product;
import org.dsa11.team1.kumarketto.domain.entity.ProductDocument;
import org.dsa11.team1.kumarketto.repository.ProductElasticsearchRepository;
import org.dsa11.team1.kumarketto.repository.ProductRepository;
import org.dsa11.team1.kumarketto.repository.WishListRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
@Order(2)
public class DataLoader implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final ProductElasticsearchRepository productElasticsearchRepository;
    private final WishListRepository wishListRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        if (productElasticsearchRepository.count() == 0) {
            log.info("Elasticsearch is empty. Seeding data from RDBMS...");

            // 2-1. DB에서 모든 상품을 가져옵니다.
            List<Product> allProducts = productRepository.findAll();
            if (allProducts.isEmpty()) {
                log.warn("No products found in the database to seed.");
                return;
            }

            // 2-2. 모든 상품의 ID 리스트를 만듭니다.
            List<Long> allProductIds = allProducts.stream().map(Product::getPid).toList();

            // 2-3. 찜 개수를 한 번의 쿼리로 모두 가져와 Map에 저장합니다. (효율적)
            List<Object[]> likeCounts = wishListRepository.countLikesByProductIds(allProductIds);
            Map<Long, Long> likeCountMap = likeCounts.stream()
                    .collect(Collectors.toMap(
                            row -> (Long) row[0], // productId
                            row -> (Long) row[1]  // likeCount
                    ));

            // 2-4. 각 상품에 대해 올바른 likeCount를 포함한 ProductDocument를 만듭니다.
            List<ProductDocument> documentsToSave = allProducts.stream()
                    .map(product -> {
                        long likeCount = likeCountMap.getOrDefault(product.getPid(), 0L);
                        // ProductDocument.fromProduct() static 메소드를 사용합니다.
                        return ProductDocument.fromProduct(product, likeCount);
                    })
                    .toList();

            // 2-5. Elasticsearch에 한번에 모두 저장합니다.
            if (!documentsToSave.isEmpty()) {
                productElasticsearchRepository.saveAll(documentsToSave);
                log.info("Successfully seeded {} products to Elasticsearch.", documentsToSave.size());
            }

        } else {
            log.info("Elasticsearch already has data. Seeding skipped.");
        }

    }

}
