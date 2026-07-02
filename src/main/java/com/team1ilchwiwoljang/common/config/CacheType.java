package com.team1ilchwiwoljang.common.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CacheType {
    POPULAR_PRODUCTS("popularProducts", 10); // 캐시 이름, 만료 시간(분)

    private final String cacheName;
    private final int expiredAfterWriteMin;
}
