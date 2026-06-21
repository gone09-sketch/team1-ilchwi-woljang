# Controller

## 목적

Controller는 HTTP 요청과 응답을 담당한다.

비즈니스 규칙을 직접 판단하지 않고, 요청을 검증한 뒤 Service를 호출하고 응답을 반환하는 역할만 가진다.

## 기본 원칙

- Controller는 얇게 유지한다.
- 비즈니스 로직은 Controller에 작성하지 않는다.
- Entity를 API 응답으로 직접 반환하지 않는다.
- Request DTO와 Response DTO를 사용한다.
- 예외 처리는 Controller에서 반복하지 않고 공통 예외 처리 방식을 따른다.
- 응답 형식과 HTTP 상태 코드는 `api-response.md`를 따른다.

## Controller에서 할 일

- URL, HTTP Method 매핑
- PathVariable, RequestParam, RequestBody 수신
- `@Valid`를 통한 요청 형식 검증
- 인증 사용자 정보 전달
- Service 호출
- Service가 반환한 DTO를 응답으로 반환

## Controller에서 하지 않을 일

- 비즈니스 규칙 판단
- Entity 상태 변경
- Repository 직접 호출
- DB 접근
- 외부 API 직접 호출
- 트랜잭션 처리
- 복잡한 조건 분기
- 반복적인 try-catch 예외 처리

## 요청 처리

요청 본문이 필요한 경우 Request DTO를 사용한다.

요청 본문이 필요 없는 단순 조회, 삭제 요청은 `@PathVariable`, `@RequestParam`을 사용할 수 있다.

복잡한 검색 조건은 별도 Search Request DTO 사용을 검토한다.

요청 값 검증 기준은 `validation.md`를 따른다.

## 인증 사용자 정보

인증이 필요한 API는 Security에서 검증된 사용자 정보를 전달받는다.

구체적인 인증 사용자 전달 방식은 `security.md`를 따른다.

Controller는 인증 사용자 전체 객체보다 필요한 식별자 값을 Service에 전달하는 것을 우선한다.

예시:

- `userId`
- `email`
- `role`

## 응답 처리

Controller는 Service가 반환한 Response DTO를 그대로 응답하는 것을 우선한다.

Controller에서 Entity를 DTO로 변환하지 않는다.

단순한 HTTP 상태 코드 지정이나 공통 응답 래핑은 Controller에서 처리할 수 있다.

구체적인 응답 형식과 HTTP 상태 코드 기준은 `api-response.md`를 따른다.

## 예외 처리

Controller에서 같은 try-catch를 반복하지 않는다.

예상 가능한 실패는 Service에서 예외로 표현하고, 공통 예외 처리에서 응답으로 변환한다.

구체적인 예외 처리 기준은 `exception.md`를 따른다.

## 피해야 할 Controller

- Service 없이 Controller에서 모든 로직을 처리하는 코드
- Repository를 직접 호출하는 Controller
- Entity를 그대로 반환하는 Controller
- 요청 검증, 비즈니스 검증, 응답 조립이 모두 섞인 Controller
- API마다 try-catch를 반복하는 Controller