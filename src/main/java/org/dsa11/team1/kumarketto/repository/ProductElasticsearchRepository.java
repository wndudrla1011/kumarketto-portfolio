package org.dsa11.team1.kumarketto.repository;

import org.dsa11.team1.kumarketto.domain.entity.ProductDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface ProductElasticsearchRepository extends ElasticsearchRepository<ProductDocument, Long> {

    @Query("""
            {
                "function_score": {
                    "query": {
                        "term": {
                            "status": "NEW"
                        }
                    },
                    "functions": [
                        {
                            "filter": {"match_all": {}},
                            "gauss": {
                                "modifiedDate": {
                                    "origin": "now",
                                    "scale": "30d",
                                    "offset": "7d",
                                    "decay": 0.5
                                }
                            },
                            "weight": 1.5
                        },
                        {
                            "filter": {"match_all": {}},
                            "field_value_factor": {
                                "field": "viewCount",
                                "modifier": "ln1p",
                                "missing": 1
                            },
                            "weight": 1.0
                        },
                        {
                            "filter": {"match_all": {}},
                            "field_value_factor": {
                                "field": "likeCount",
                                "modifier": "ln1p",
                                "missing": 1
                            },
                            "weight": 1.2
                        }
                    ],
                    "score_mode": "sum",
                    "boost_mode": "sum"
                }
            }
            """)
    List<ProductDocument> findByCustomScore();

}
