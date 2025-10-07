package org.dsa11.team1.kumarketto.config;

import org.dsa11.team1.kumarketto.domain.entity.ProductDocument;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Profile("dev")
public class IndexInitializer {

    @Bean
    @Order(1)
    public CommandLineRunner createIndexAndMapping(ElasticsearchOperations elasticsearchOperations) {
        return args -> {
            String indexName = "products";

            // 개발 환경에서는 시작할 때마다 인덱스를 지우고 새로 생성
            if (elasticsearchOperations.indexOps(IndexCoordinates.of(indexName)).exists()) {
                elasticsearchOperations.indexOps(IndexCoordinates.of(indexName)).delete();
            }

            // 인덱스 생성 (쿠로모지 분석기 설정 포함)
            Map<String, Object> settings = createKuromojiSettings();
            elasticsearchOperations.indexOps(IndexCoordinates.of(indexName)).create(settings);

            // 매핑 적용
            elasticsearchOperations.indexOps(IndexCoordinates.of(indexName)).putMapping(
                    elasticsearchOperations.indexOps(ProductDocument.class).createMapping()
            );

            System.out.println("Elasticsearch index and mapping for 'products' created successfully with Kuromoji analyzer.");
        };
    }

    private Map<String, Object> createKuromojiSettings() {
        // 쿠로모지 커스텀 분석기 정의
        Map<String, Object> kuromojiAnalyzer = new HashMap<>();
        kuromojiAnalyzer.put("type", "custom");
        kuromojiAnalyzer.put("tokenizer", "kuromoji_tokenizer");
        kuromojiAnalyzer.put("filter", new String[]{"kuromoji_baseform", "kuromoji_part_of_speech", "cjk_width", "lowercase", "kuromoji_stemmer"});

        Map<String, Object> kuromojiTokenizer = new HashMap<>();
        kuromojiTokenizer.put("type", "kuromoji_tokenizer");
        kuromojiTokenizer.put("mode", "normal");

        Map<String, Object> analysis = new HashMap<>();
        analysis.put("analyzer", Map.of("kuromoji", kuromojiAnalyzer));
        analysis.put("tokenizer", Map.of("kuromoji_tokenizer", kuromojiTokenizer));

        return Map.of("analysis", analysis);
    }
}