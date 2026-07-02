package com.team1ilchwiwoljang.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * products.name 컬럼의 FULLTEXT(ngram) 인덱스를 앱 시작 시 확인하고 없으면 생성합니다.
 * JPA @Index는 BTREE만 지원하고, 이 프로젝트는 별도 마이그레이션 도구를 쓰지 않아서
 * 이 인덱스는 ddl-auto로 자동 반영되지 않습니다.
 */
@Component
@RequiredArgsConstructor
public class ProductFullTextIndexInitializer implements ApplicationRunner {

    private static final String INDEX_NAME = "idx_product_name_fulltext";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.statistics " +
                        "WHERE table_schema = DATABASE() AND table_name = 'products' " +
                        "AND index_name = ?",
                Integer.class,
                INDEX_NAME
        );

        if (count == null || count == 0) {
            jdbcTemplate.execute(
                    "ALTER TABLE products ADD FULLTEXT INDEX " + INDEX_NAME + " (name) WITH PARSER ngram"
            );
        }
    }
}
