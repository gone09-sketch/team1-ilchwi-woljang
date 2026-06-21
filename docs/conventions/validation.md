# Validation

## 목적

요청 값 검증과 비즈니스 규칙 검증을 구분해 책임을 명확히 한다.

잘못된 요청은 일관된 에러 응답으로 반환한다.

## 기본 원칙

- 요청 값의 형식 검증은 Request DTO에서 처리한다.
- 비즈니스 규칙 검증은 Service에서 처리한다.
- Controller에서는 `@Valid`를 통해 Request DTO 검증을 실행한다.
- 검증 실패 응답 형식은 `api-response.md`를 따른다.
- validation 예외 처리 기준은 `exception.md`를 따른다.

## Request DTO에서 검증할 것

Request DTO에서는 입력 값 자체의 형식을 검증한다.

예시:

- 필수 값 누락
- 빈 문자열
- 이메일 형식
- 문자열 길이
- 숫자 범위
- 날짜 형식
- Enum 값 형식

## Service에서 검증할 것

Service에서는 DB 조회나 현재 상태 확인이 필요한 비즈니스 규칙을 검증한다.

예시:

- 중복 이메일
- 존재하지 않는 리소스
- 본인 소유 데이터 여부
- 주문 가능 상태 여부
- 재고 부족 여부
- 이미 처리된 요청 여부

## PathVariable과 RequestParam 검증

`@PathVariable`, `@RequestParam`도 필요한 경우 검증한다.

예시:

- id는 양수여야 한다.
- page는 0 이상이어야 한다.
- size는 허용된 범위 안이어야 한다.
- 검색어는 최대 길이를 넘지 않아야 한다.

## 검증 실패 응답

Request DTO 검증 실패, PathVariable 검증 실패, RequestParam 검증 실패는 공통 validation 실패 응답으로 처리한다.

기본 에러 코드는 `VALIDATION_FAILED`를 사용한다.

Enum 값 변환 실패처럼 허용되지 않은 값이 들어온 경우는 `INVALID_ENUM_VALUE` 사용을 검토한다.

## 피해야 할 Validation

- Controller에서 if문으로 반복 검증하는 방식
- Request DTO에서 DB 조회가 필요한 비즈니스 검증을 수행하는 방식
- Service에서 단순 형식 검증만 반복하는 방식
- 검증 실패 응답 형식이 API마다 다른 구조