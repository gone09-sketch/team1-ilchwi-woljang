package com.team1ilchwiwoljang.domain.order.repository;

import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ActiveProfiles("test")
@SpringBootTest
public class OrderIndexPerformanceTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long targetMemberId;
    private List<Long> otherMemberIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // 기존 데이터 정리
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.execute("TRUNCATE TABLE orders");
        jdbcTemplate.execute("TRUNCATE TABLE members");
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");

        // 테스트 멤버 생성
        Member target = Member.create("target@test.com", "pass", "Target", "010-1234-5678");
        memberRepository.save(target);
        targetMemberId = target.getId();

        for (int i = 1; i <= 4; i++) {
            Member other = Member.create("other" + i + "@test.com", "pass", "Other" + i, "010-1234-567" + i);
            memberRepository.save(other);
            otherMemberIds.add(other.getId());
        }

        // 인덱스 있을 수 있으니 드롭
        jdbcTemplate.execute("DROP INDEX IF EXISTS idx_orders_member_id_id");

        System.out.println(">>> 대량 주문 데이터 삽입 중... (총 80,000건)");

        // Target 회원 주문 40,000건 삽입
        insertOrdersBatch(targetMemberId, 40000);

        // 다른 회원들 주문 각 10,000건씩 삽입 (총 40,000건)
        for (Long otherId : otherMemberIds) {
            insertOrdersBatch(otherId, 10000);
        }

        System.out.println(">>> 데이터 삽입 완료!");
    }

    private void insertOrdersBatch(Long memberId, int count) {
        String sql = "INSERT INTO orders (member_id, order_number, total_amount, order_status, pg_amount, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        List<Object[]> batchArgs = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < count; i++) {
            batchArgs.add(new Object[]{
                    memberId,
                    UUID.randomUUID().toString().substring(0, 20),
                    10000L + (i * 10),
                    "PENDING",
                    10000L + (i * 10),
                    Timestamp.valueOf(now.minusMinutes(i)),
                    Timestamp.valueOf(now)
            });

            if (batchArgs.size() == 5000) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
                batchArgs.clear();
            }
        }
        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batchArgs);
        }
    }

    @Test
    void comparePerformance() {
        int repetitions = 100;
        // 페이징 조건 설정 (뒤쪽 페이지 조회 유도)
        int limit = 10;
        int offset = 20000;

        String query = "SELECT * FROM orders WHERE member_id = ? ORDER BY id DESC LIMIT ? OFFSET ?";

        // 1. Warm-up
        for (int i = 0; i < 10; i++) {
            jdbcTemplate.queryForList(query, targetMemberId, limit, offset);
        }

        // 2. 인덱스 없는 상태에서 성능 측정
        long startTimeNoIndex = System.nanoTime();
        for (int i = 0; i < repetitions; i++) {
            jdbcTemplate.queryForList(query, targetMemberId, limit, offset);
        }
        long durationNoIndex = System.nanoTime() - startTimeNoIndex;
        double avgTimeNoIndexMs = (double) durationNoIndex / repetitions / 1_000_000.0;

        // 3. 인덱스 생성
        System.out.println(">>> 복합 인덱스 (member_id, id DESC) 생성 중...");
        long indexCreateStart = System.currentTimeMillis();
        jdbcTemplate.execute("CREATE INDEX idx_orders_member_id_id ON orders(member_id, id DESC)");
        System.out.println(">>> 인덱스 생성 완료! (소요 시간: " + (System.currentTimeMillis() - indexCreateStart) + "ms)");

        // 4. Warm-up (인덱스 적용 후)
        for (int i = 0; i < 10; i++) {
            jdbcTemplate.queryForList(query, targetMemberId, limit, offset);
        }

        // 5. 인덱스 있는 상태에서 성능 측정
        long startTimeWithIndex = System.nanoTime();
        for (int i = 0; i < repetitions; i++) {
            jdbcTemplate.queryForList(query, targetMemberId, limit, offset);
        }
        long durationWithIndex = System.nanoTime() - startTimeWithIndex;
        double avgTimeWithIndexMs = (double) durationWithIndex / repetitions / 1_000_000.0;

        // 6. 결과 출력
        System.out.println("==================================================");
        System.out.println("          [인덱스 적용 성능 비교 결과]");
        System.out.println("==================================================");
        System.out.printf("- 인덱스 미적용 평균 실행 시간: %.4f ms\n", avgTimeNoIndexMs);
        System.out.printf("- 인덱스 적용 후 평균 실행 시간: %.4f ms\n", avgTimeWithIndexMs);
        System.out.printf("- 성능 개선 배율: %.2f 배 빨라짐\n", (avgTimeNoIndexMs / avgTimeWithIndexMs));
        System.out.println("==================================================");
    }
}
