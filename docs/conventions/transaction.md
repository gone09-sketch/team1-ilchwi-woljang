# Transaction

## 목적

트랜잭션은 여러 DB 작업을 하나의 성공/실패 단위로 묶기 위해 사용한다.

데이터 정합성이 깨지지 않도록 트랜잭션 범위를 명확히 정한다.

## 기본 원칙

- DB 상태가 변경되는 Service 메서드는 `@Transactional` 적용을 검토한다.
- 같이 성공하거나 같이 실패해야 하는 DB 작업은 하나의 트랜잭션 안에 둔다.
- 조회 전용 메서드는 `@Transactional(readOnly = true)` 사용을 검토한다.
- Controller나 Repository가 아니라 Service 계층에서 트랜잭션 경계를 관리한다.
- 트랜잭션 안에서 오래 걸리는 외부 작업을 수행하지 않도록 주의한다.

## 적용 위치

트랜잭션은 Service 계층에 적용한다.

Controller에는 트랜잭션을 적용하지 않는다.

Repository는 DB 접근을 담당하고, 전체 비즈니스 흐름의 트랜잭션 경계는 Service에서 관리한다.

## readOnly 기준

조회만 수행하는 메서드는 `readOnly = true` 사용을 검토한다.

DB 상태 변경이 있는 메서드에는 `readOnly = true`를 사용하지 않는다.

## 롤백 기준

전체 작업이 실패해야 하는 예외는 catch로 숨기지 않고 밖으로 던진다.

예외를 catch한 뒤 정상 흐름처럼 끝내야 한다면, 그 예외가 전체 작업 실패 사유가 아닌지 먼저 판단한다.

Spring의 기본 트랜잭션은 주로 unchecked exception에서 rollback된다.

checked exception에 대한 rollback이 필요하면 별도 설정을 검토한다.

## 트랜잭션 범위

트랜잭션 범위는 데이터 정합성에 필요한 만큼만 잡는다.

아래 작업은 트랜잭션 안에 둘지 신중하게 판단한다.

- 외부 API 호출
- 이메일 발송
- 알림 전송
- 파일 업로드
- 오래 걸리는 계산

## 도메인 간 Service 호출

도메인 간 Service 호출 시 기본 전파 방식은 `REQUIRED`를 우선 따른다.

별도 트랜잭션이 필요할 때만 전파 옵션 변경을 검토한다.

전파 옵션 변경은 데이터 정합성에 미치는 영향을 설명할 수 있을 때만 사용한다.

## 테스트 기준

트랜잭션 롤백 테스트 기준은 `test-convention.md`를 따른다.

## 피해야 할 Transaction

- Controller에 `@Transactional`을 붙이는 방식
- 트랜잭션 안에서 오래 걸리는 외부 API를 호출하는 방식
- 예외를 catch하고 정상 종료해 rollback을 막는 방식
- 이유 없이 전파 옵션을 변경하는 방식
- 조회 전용 메서드에 불필요하게 쓰기 트랜잭션을 사용하는 방식