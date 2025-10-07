package org.dsa11.team1.kumarketto.util;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dsa11.team1.kumarketto.service.CategoryService;
import org.dsa11.team1.kumarketto.service.LocationService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheWarmer {

    private final LocationService locationService;
    private final CategoryService categoryService;

    /**
     * 캐싱해야 할 데이터를 미리 캐싱
     */
    @PostConstruct // 모든 DI 를 마친 후 딱 1회 실행
    @Async // 별도의 백그라운드 스레드로 비동기 실행
    public void prewarmCaches() {
        log.info("캐시 예열을 시작합니다...");

        try {
            locationService.getHierarchicalLocations();
            categoryService.getHierarchicalCategories();
            log.info("캐시 예열 완료");
        } catch (Exception e) {
            log.error("캐시 예열 중 오류 발생", e);
        }
    }

}
