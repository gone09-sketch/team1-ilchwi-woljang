package com.team1ilchwiwoljang.domain.search.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class PopularKeywordCacheScheduler {

    private final PopularKeywordCacheService popularKeywordCacheService;

    /**
     * 9분 30초 주기로 @CachePut 웜업 메서드를 호출하여 Redis 캐시 TTL을 강제 리셋합니다.
     * (캐시 만료 시간 10분보다 짧은 주기로 실행하여 캐시 스탬피드를 방어합니다.)
     */
    @Scheduled(fixedDelay = 570000)
    public void warmUpPopularKeywords() {
        log.info("인기 검색어 캐시 웜업(Cache Warm-up) 백그라운드 스케줄러 작동 시작...");

        popularKeywordCacheService.warmUpPopularKeywordsCache();

        log.info("인기 검색어 캐시 웜업 완료. (Key: all)");
    }
}
