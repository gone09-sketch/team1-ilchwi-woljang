# Repository

## 목적

Repository는 DB 접근을 담당한다.

비즈니스 규칙을 판단하지 않고, Entity 저장과 조회를 위한 데이터 접근 역할만 가진다.

## 기본 원칙

- 기본 CRUD는 Spring Data JPA 기본 메서드를 우선 사용한다.
- 복잡한 조회 조건이 필요할 때만 직접 쿼리를 작성한다.
- Repository에는 비즈니스 규칙을 넣지 않는다.
- 대량 조회는 페이징을 우선 검토한다.
- 직접 작성한 쿼리는 조건, 정렬, 페이징, 연관관계를 명확히 드러낸다.

## Repository에서 할 일

- Entity 저장
- Entity 조회
- 조건 기반 조회
- 정렬, 페이징 조회
- 직접 작성한 쿼리 실행

## Repository에서 하지 않을 일

- 비즈니스 정책 판단
- 예외 응답 생성
- DTO 응답 조립
- 외부 API 호출
- 트랜잭션 흐름 결정

## 메서드 이름

Spring Data JPA 메서드 이름은 의미가 명확할 때만 사용한다.

조건이 너무 길어지면 직접 쿼리나 QueryDSL 도입을 검토한다.

예시:

- `findByEmail`
- `existsByEmail`
- `findByUserIdOrderByCreatedAtDesc`

너무 긴 메서드명은 피한다.

## 직접 쿼리 작성 기준

아래 경우 직접 쿼리 작성을 검토한다.

- 조건이 복잡한 조회
- 여러 테이블 조인이 필요한 조회
- fetch join이 필요한 조회
- 페이징과 정렬 조건이 복잡한 조회
- 성능상 필요한 DTO 조회

직접 쿼리를 작성한 경우 테스트 필요성을 함께 검토한다.

## N+1 주의

연관관계를 조회할 때 N+1 문제가 발생할 수 있는지 확인한다.

필요한 경우 아래 방식을 검토한다.

- fetch join
- EntityGraph
- DTO 직접 조회
- 필요한 연관 데이터만 별도 조회

## Custom Repository

QueryDSL 등으로 커스텀 Repository가 필요하면 Naming 문서의 Repository Custom 이름 규칙을 따른다.

예시:

- `UserRepository`
- `UserRepositoryCustom`
- `UserRepositoryCustomImpl`

## 피해야 할 Repository

- 비즈니스 로직이 들어간 Repository
- 단순 조회에 불필요하게 Entity 전체를 가져오는 쿼리
- 너무 긴 Spring Data JPA 메서드명
- 페이징 없는 대량 조회
- 사용하지 않는 연관관계를 함께 가져오는 쿼리
```