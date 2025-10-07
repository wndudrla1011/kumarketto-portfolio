package org.dsa11.team1.kumarketto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing // JPA Auditing
@EnableElasticsearchRepositories // Elasticsearch
@SpringBootApplication
@EnableScheduling // Batch Job
@EnableCaching // Caching
@EnableAsync // Async
public class KumarkettoApplication {

	public static void main(String[] args) {
		SpringApplication.run(KumarkettoApplication.class, args);
	}

}
