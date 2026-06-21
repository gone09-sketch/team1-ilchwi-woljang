# Service

## 목적

Service는 비즈니스 규칙과 트랜잭션 흐름을 담당한다.

Controller에서 받은 요청을 바탕으로 도메인 정책을 판단하고, Entity 상태 변경과 Repository 호출을 조합한다.

## 기본 원칙

- 비즈니스 로직은 Service에 둔다.
- DB 상태가 변경되는 작업은 트랜잭션 범위를 검토한다.
- Controller 전용 책임을 Service에 넣지 않는다.
- Repository를 호출해 필요한 데이터를 조회하고 저장한다.
- Entity 상태 변경은 의미 있는 메서드를 통해 수행한다.
- Response DTO 변환은 기본적으로 Service 계층에서 수행한다.

## Service에서 할 일

- 비즈니스 규칙 판단
- Entity 조회와 상태 변경
- Repository 호출
- 트랜잭션 경계 관리
- 예상 가능한 실패 상황에서 예외 발생
- Response DTO 생성 또는 반환

## Service에서 하지 않을 일

- HTTP 요청/응답 직접 처리
- HTTP Status Code 직접 판단
- 화면 표시용 데이터 포맷팅
- Controller의 `@RequestBody`, `@PathVariable`에 직접 의존
- 불필요한 외부 API 호출을 트랜잭션 안에 오래 유지

## 도메인 간 Service 호출

다른 도메인의 데이터나 상태 변경이 필요하면 해당 도메인의 Service를 통해 요청한다.

도메인 간 Service 호출 시 트랜잭션 전파 방식은 기본 전파(`REQUIRED`)를 우선 따른다.

별도 트랜잭션이 필요한 경우는 `transaction.md` 기준을 따른다.

## 트랜잭션

같이 성공하거나 같이 실패해야 하는 DB 작업은 하나의 트랜잭션 안에 둔다.

조회 전용 메서드는 `readOnly = true` 사용을 검토한다.

외부 API 호출, 이메일 발송, 알림 전송처럼 실패 가능성이 있거나 오래 걸리는 작업은 트랜잭션 안에 둘지 신중하게 판단한다.

전체 작업이 실패해야 하는 예외는 catch로 숨기지 않고 밖으로 던져 트랜잭션이 롤백되게 한다.

예외를 catch한 뒤 정상 흐름처럼 끝내야 한다면, 그 예외가 전체 작업 실패 사유가 아닌지 먼저 판단한다.

자세한 기준은 `transaction.md`를 따른다.

## DTO 반환

Service는 Entity를 Controller에 그대로 반환하지 않는다.

기본적으로 Service에서 Entity를 Response DTO로 변환해 반환한다.

단순 변환은 Response DTO의 `from()` 메서드를 우선 사용한다.

복잡한 변환이 반복되면 별도 Mapper 도입을 검토한다.

## 예외 처리

Service는 비즈니스 규칙 위반을 명확한 예외로 표현한다.

예시:

- 중복 이메일
- 존재하지 않는 리소스
- 권한 없는 접근
- 소유자가 아닌 사용자의 수정 요청
- 이미 처리된 요청
- 처리할 수 없는 상태 변경

예외 응답 형식은 `exception.md`, `api-response.md`를 따른다.

## 메서드 작성 기준

하나의 Service 메서드는 하나의 핵심 작업을 수행한다.

메서드가 너무 길어지면 검증, 조회, 상태 변경, 응답 생성 흐름을 분리할 수 있는지 검토한다.

단, 의미 없는 private 메서드 분리는 피한다.

## 피해야 할 Service

- 모든 기능이 하나의 큰 메서드에 몰린 Service
- Controller 요청/응답 형식에 강하게 묶인 Service
- Repository 없이 Entity를 임의로 생성하고 끝내는 Service
- 외부 API 호출을 트랜잭션 안에 오래 묶어두는 Service
- 예외를 catch한 뒤 아무 처리 없이 무시하는 Service