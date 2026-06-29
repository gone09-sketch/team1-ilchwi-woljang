# 🧪 주문 내역 동적 필터 검색 성능 및 부하 테스트 가이드

본 문서는 인덱스 미적용 상태와 적용 상태의 차이를 **로컬/dev 환경에서 직접 실험하고 기록하기 위한 가이드 및 리포트 템플릿**입니다. 포스트맨을 사용하여 동적 쿼리 API 호출 시 발생하는 성능 병목을 몸소 체감하고 문서화하는 것을 목표로 합니다.

---

## 1. 테스트 하드웨어 스펙 기록

부하 테스트를 진행하는 본인 PC/서버의 스펙을 작성합니다. (환경마다 절대 속도가 다르게 측정되므로 필수 기록)

* **테스트 일시**: 2026-06-25
* **CPU**: 
* **Memory**: 
* **Storage Type (SSD/HDD)**: 
* **OS**: 
* **RDBMS Version (MySQL/H2)**: 

---

## 2. 테스트 데이터 적재 (Seed Data)

부하와 불편함을 유의미하게 체감하려면 최소 **100,000건 이상의 주문 데이터**를 적재해야 합니다.
MySQL 워크벤치나 DBeaver 등 DB 클라이언트 툴을 통해 아래 스크립트(프로시저)를 실행하여 테스트용 대량 데이터를 적재하세요.

### 2.1 대량 데이터 생성 프로시저 (MySQL 전용)
```sql
DELIMITER $$
CREATE PROCEDURE insert_bulk_orders()
BEGIN
    DECLARE i INT DEFAULT 1;
    -- 1번 회원에게 50,000건의 주문 데이터 생성
    WHILE i <= 50000 DO
        INSERT INTO orders (member_id, order_number, total_amount, order_status, pg_amount, created_at, updated_at)
        VALUES (
            1, 
            CONCAT('ORD-MATCH-', LPAD(i, 6, '0')), 
            10000 + (i % 100), 
            IF(i % 10 = 0, 'CANCELLED', 'PENDING'), 
            10000 + (i % 100), 
            DATE_SUB(NOW(), INTERVAL i MINUTE), 
            NOW()
        );
        SET i = i + 1;
    END WHILE;
    
    -- 다른 회원들(2~5번)의 무작위 주문 데이터 50,000건 생성
    SET i = 1;
    WHILE i <= 50000 DO
        INSERT INTO orders (member_id, order_number, total_amount, order_status, pg_amount, created_at, updated_at)
        VALUES (
            FLOOR(2 + (RAND() * 4)), 
            CONCAT('ORD-OTHER-', LPAD(i, 6, '0')), 
            20000, 
            'PENDING', 
            20000, 
            DATE_SUB(NOW(), INTERVAL i MINUTE), 
            NOW()
        );
        SET i = i + 1;
    END WHILE;
END$$
DELIMITER ;

-- 프로시저 실행
CALL insert_bulk_orders();
```

---

## 3. 포스트맨(Postman) 호출 명세

주문 내역 동적 검색 API의 포스트맨 설정 가이드입니다.

* **Method**: `GET`
* **URL**: `http://localhost:8080/api/orders`
* **Headers**:
  * `Authorization`: `Bearer {로그인시 발급받은 JWT Access Token}`
* **Query Parameters (동적 쿼리 조합)**:

| Key | Value 예시 | 설명 |
| :--- | :--- | :--- |
| `startDate` | `2026-06-01` | 검색 시작 날짜 |
| `endDate` | `2026-06-25` | 검색 종료 날짜 |
| `orderStatus`| `CANCELLED` | 주문 상태 (`PENDING`, `CANCELLED` 등) |
| `keyword`| `MATCH` | 주문 번호 검색어 (부분 일치) |
| `page` | `0` | 페이지 번호 (0부터 시작) |
| `size` | `10` | 페이지당 노출 개수 |

---

## 4. 인덱싱 불편함 체감 및 성능 측정 실험 시나리오

아래 두 단계를 순서대로 진행하며 쿼리 응답 시간과 로컬 PC의 CPU 팬 소리(부하)를 관찰합니다.

### 4.1 [실험 1] 인덱스가 없는 상태 (불편함 체감)
1. 로컬 DB에서 기존 복합 인덱스를 제거합니다.
   ```sql
   ALTER TABLE orders DROP INDEX idx_orders_member_id_created_at_id;
   ```
2. 포스트맨을 켜고 아래 3가지 조합의 API를 각각 **5회 이상** 호출하여 응답 속도의 최고/최저/평균 값을 관찰합니다.
3. 데이터베이스 프로세스(`mysqld`)의 CPU 점유율을 함께 모니터링합니다.

### 4.2 [실험 2] 복합 인덱스 적용 상태 (성능 개선)
1. 로컬 DB에 튜닝된 복합 인덱스를 생성합니다.
   ```sql
   ALTER TABLE orders ADD INDEX idx_orders_member_id_created_at_id (member_id, created_at DESC, id DESC);
   ```
2. 실험 1과 동일한 조건으로 포스트맨 API를 다시 호출하여 성능 개선 효과를 비교합니다.

---

## 5. 성능 비교 테스트 기록표 (템플릿)

실험 1과 실험 2를 진행하며 측정한 평균 응답 속도(Response Time)를 아래 표에 기입하여 성능 병목 개선치를 증명하세요.

| 호출 시나리오 | 쿼리 파라미터 조건 | 인덱스 미적용 (ms) | 인덱스 적용 (ms) | 개선 성능 (배) |
| :--- | :--- | :---: | :---: | :---: |
| **1. 전체 조회** | `page=0&size=10` | | | |
| **2. 기간 필터 검색** | `startDate=2026-06-01&endDate=2026-06-25` | | | |
| **3. 상태 필터 검색** | `orderStatus=CANCELLED` | | | |
| **4. 복합 필터 검색** | `orderStatus=CANCELLED&keyword=CANCELLED` | | | |
| **5. 깊은 페이징 조회**| `page=4000&size=10` (뒤쪽 데이터 조회) | | | |

---

## 6. 결론 및 느낀 점 (실험 후 작성)
* *예시: 인덱스 미적용 시 40,000번째 데이터를 조회하는 깊은 페이징(OFFSET 40000)에서 응답 속도가 X.X초까지 폭증하여 웹 프론트엔드가 타임아웃을 겪는 불편함이 발생했으나, 복합 인덱스 적용 후 Filesort가 제거되어 0.0X초대로 응답 속도가 대폭 향상됨을 확인함.*
