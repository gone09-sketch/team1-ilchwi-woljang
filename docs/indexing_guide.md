# 📑 데이터베이스 인덱싱 설계 및 검증 가이드 (Database Indexing Guide)

본 문서는 서비스의 성능 최적화를 위한 데이터베이스 인덱싱 전략과 검증 방법을 정리한 문서입니다. 특히 **주문 내역 조회**와 **상품 목록 조회** 기능의 정렬 및 페이징 성능 극대화를 목표로 합니다.

---

## 1. 인덱싱 도입 배경 및 목적

서비스 규모가 확장되고 데이터가 누적됨에 따라, 특정 조건의 조회나 대량 데이터 정렬(Filesort) 작업은 데이터베이스 CPU 사용량을 폭증시키고 응답 속도를 저하시키는 주원인이 됩니다.
* **성능 병목 해결**: `WHERE` 필터링 조건과 `ORDER BY` 정렬 조건이 얽힌 페이징 쿼리를 인덱스를 통해 테이블 풀 스캔(Table Full Scan) 없이 빠르게 처리합니다.
* **정렬 비용 최소화**: 디스크나 메모리 상에서 수행되는 정렬(Filesort) 과정을 생략하고, 인덱스가 정렬해 놓은 순서 그대로 데이터를 읽어오도록 유도합니다.

---

## 2. 주문 내역 조회 인덱싱 전략 (`orders` 테이블)

### 2.1 주요 실행 쿼리 분석
회원이 자신의 주문 내역 목록을 최신순(역순)으로 조회할 때 호출되는 주요 SQL 형태는 다음과 같습니다.
```sql
SELECT * FROM orders 
WHERE member_id = :memberId 
ORDER BY created_at DESC, id DESC 
LIMIT :limit OFFSET :offset;
```

### 2.2 인덱스 설계: `(member_id, created_at, id)` 복합 인덱스
* **인덱스 명칭**: `idx_orders_member_id_created_at_id`
* **설계 기준**:
  * **동등 조건 필터링 우선**: `WHERE` 절에 사용되는 동등(=) 조건 컬럼인 `member_id`를 복합 인덱스의 첫 번째 컬럼으로 지정합니다.
  * **정렬 순서(Order By) 일치**: `ORDER BY`에 사용되는 정렬 기준 컬럼인 `created_at DESC, id DESC`를 뒤에 배치하여 정렬(Filesort)을 회피합니다.
  * **안정적인 최신순 보장**: `created_at`이 같은 주문이 있을 수 있으므로 `id DESC`를 보조 정렬 기준으로 둡니다.
* **InnoDB 특성을 고려한 원리**:
  * MySQL의 InnoDB 스토리지 엔진에서 보조 인덱스(Secondary Index)는 항상 내부적으로 PK(여기서는 `id`)를 리프 노드에 포함하고 있습니다.
  * 다만 이번 조회의 기본 정렬 기준은 `id` 단독이 아니라 `created_at DESC, id DESC`이므로, 정렬 기준까지 명시한 `(member_id, created_at DESC, id DESC)` 복합 인덱스를 사용합니다.

---

## 3. 상품 목록 조회 인덱싱 전략 (`products` 테이블)

### 3.1 주요 실행 쿼리 및 조건 분석
상품 목록 조회는 다양한 필터링 조건과 정렬(인기순, 판매순, 최신순, 가격순) 조건이 결합됩니다.

#### A. 카테고리별 상품 최신순 페이징 조회
```sql
SELECT * FROM products 
WHERE category_id = :categoryId AND status = 'ON_SALE' 
ORDER BY created_at DESC 
LIMIT :limit OFFSET :offset;
```

#### B. 인기순 / 판매량순 정렬 조회
```sql
SELECT * FROM products 
WHERE status = 'ON_SALE' 
ORDER BY wish_count DESC, id DESC -- (예시: 인기순)
LIMIT :limit OFFSET :offset;
```

### 3.2 복합 인덱스 추천 설계
상품 테이블은 Cardinality(값의 고유도)가 높은 조건과 정렬 대상 필드를 적절히 조합해야 합니다.

1. **카테고리별 판매 상품 최신순 조회 최적화**
   * **추천 인덱스**: `(status, category_id, created_at)` 또는 `(category_id, status, created_at)`
   * **원리**: `status = 'ON_SALE'`과 `category_id = :categoryId`로 데이터의 범위를 먼저 좁힌 다음, 이미 인덱스로 정렬되어 있는 `created_at` 순서대로 빠르게 접근하여 Filesort를 방지합니다.

2. **인기순 / 판매량순 정렬 조회 최적화**
   * **추천 인덱스**: `(status, wish_count, id)` 또는 `(status, sales_volume, id)`
   * **원리**: 노출 조건(`status`)이 첫 번째 순서로 오고, 정렬 기준이 되는 통계성 수치 컬럼(`wish_count`, `sales_volume`)이 그 뒤를 이어 결합 인덱스를 구성합니다.

> 💡 **Tip**: 상품 검색의 경우 `LIKE '%keyword%'` 조회가 잦다면 B-Tree 인덱스가 작동하지 않으므로, 성능 고도화를 위해서는 Full-Text 인덱스를 적용하거나 별도의 검색 엔진(Elasticsearch) 도입을 검토해야 합니다.

---

## 4. EXPLAIN 실행 계획을 통한 인덱스 검증 가이드

작성한 인덱스가 실제로 쿼리 수행 시 활용되는지 확인하려면 MySQL `EXPLAIN` 키워드를 사용해 실행 계획을 검증해야 합니다.

### 4.1 체크해야 할 핵심 항목
실행 결과 테이블에서 다음 필드들을 중점적으로 확인합니다.

| 필드명 | 핵심 체크 포인트 | 의미 |
| :--- | :--- | :--- |
| **`type`** | `ref` 또는 `range` (권장) | `ALL`(Full Table Scan) 또는 `index`(Full Index Scan)인 경우 튜닝이 필요합니다. |
| **`possible_keys`** | 쿼리에 적용될 수 있는 인덱스 목록 | 후보군이 올바르게 나타나는지 확인합니다. |
| **`key`** | 실제 선택되어 사용된 인덱스 | 우리가 설계한 인덱스 명칭이 명시되어야 합니다. |
| **`rows`** | 쿼리 처리를 위해 조사해야 하는 예상 로우(Row) 수 | 이 수치가 작을수록 효율적인 쿼리입니다. |
| **`Extra`** | `Using filesort`나 `Using temporary`가 없는지 확인 | 이 문구가 나타난다면 인덱스를 통한 정렬이 실패하여 별도의 정렬 연산을 수행했다는 의미입니다. |

### 4.2 실행 예시 및 검증 명령
```sql
EXPLAIN SELECT * FROM orders 
WHERE member_id = 1 
ORDER BY created_at DESC, id DESC 
LIMIT 10;
```
* **성공적인 결과 지표**: `type: ref`, `key: idx_orders_member_id_created_at_id`, `Extra: Using index condition` (혹은 빈 값 - Using filesort가 없어야 함).

---

## 5. 인덱싱 설정 시 주의사항 (유지 관리)

* **과도한 인덱스 추가 금지**: 인덱스는 `SELECT` 조회 속도를 개선하지만, `INSERT`, `UPDATE`, `DELETE` 시마다 인덱스 트리를 재구성해야 하므로 쓰기 성능이 저하됩니다. 꼭 필요한 조회 경로에 대해서만 복합 인덱스를 설계해야 합니다.
* **인덱스 컬럼 가공 금지**: `WHERE UPPER(name) = 'APPLE'`이나 `WHERE price * 0.9 > 1000`처럼 인덱스가 걸린 컬럼을 가공하여 쿼리하면 인덱스를 사용할 수 없습니다. 반드시 `WHERE price > 1000 / 0.9` 형태로 우변을 계산하도록 쿼리를 작성해야 합니다.
* **복합 인덱스의 순서**: 복합 인덱스 `(A, B)`가 있을 때, `WHERE B = 10`처럼 선두 컬럼 `A` 없이 `B`만으로 쿼리하면 인덱스가 타지 않거나 비효율적으로 동작합니다. 반드시 첫 번째 컬럼부터 조건에 참여해야 합니다.
