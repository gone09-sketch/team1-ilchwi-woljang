# Naming

## 목적

이름만 보고 역할과 계층을 알 수 있어야 한다.

같은 종류의 클래스는 항상 같은 접미사 규칙을 따른다.

## 패키지 이름

- 전부 소문자로 쓴다.
- 언더스코어, 대문자를 쓰지 않는다.
- 패키지 이름은 의미 단위로 나눈다. (예: `user`, `chat`처럼 도메인 단위)

## 클래스 이름

- Controller: `{Domain}Controller` (예: `UserController`)
- Service: `{Domain}Service` (예: `UserService`)
- Repository: `{Domain}Repository` (예: `UserRepository`)
- Entity: 접미사 없이 도메인 명사 그대로 쓴다. (예: `User`, `ChatRoom`)
- Exception: `{상황}Exception` (예: `DuplicateEmailException`)
- Config: `{역할}Config` (예: `SecurityConfig`, `WebSocketConfig`)

## Service 인터페이스와 구현체 이름

Service 인터페이스는 기본적으로 만들지 않는다.

구현체가 여러 개 필요할 때만 인터페이스를 만든다.

인터페이스 이름은 `{Domain}Service`로 작성한다.

구현체 이름은 구현 방식이나 목적이 드러나게 작성한다.

예시:

- `UserService`
- `LocalUserService`
- `KakaoUserService`
- `DefaultNotificationService`

단순히 `UserServiceImpl`처럼 의미가 약한 이름은 가능하면 사용하지 않는다.

## DTO 이름

- 요청 DTO: `{Domain}{Action}Request` (예: `UserCreateRequest`, `ChatRoomUpdateRequest`)
- 응답 DTO: `{Domain}Response` (예: `UserResponse`)
  - 같은 도메인에 여러 응답 형태가 필요하면 목적을 붙인다. (예: `UserDetailResponse`, `UserSummaryResponse`)
- DTO는 `record` 사용을 우선 검토한다.
- 단순 Entity → DTO 변환은 Response DTO의 `from()` 메서드를 우선 사용한다.

## DTO Action 표준 단어

Request DTO의 Action은 아래 단어를 우선 사용한다.

- `Create`: 생성
- `Update`: 전체 또는 주요 정보 수정
- `Delete`: 삭제 요청
- `Search`: 검색 조건
- `Login`: 로그인
- `Signup`: 회원가입
- `Refresh`: 토큰 재발급
- `Verify`: 인증 또는 검증

예시:

- `UserCreateRequest`
- `UserUpdateRequest`
- `UserSearchRequest`
- `UserLoginRequest`

같은 의미의 단어를 섞어 쓰지 않는다. 예를 들어 `Create`와 `Register`를 같은 의미로 혼용하지 않는다.

## 공통 응답 이름

공통 성공 응답 래퍼는 `ApiResponse<T>`로 작성한다.

공통 에러 응답은 `ErrorResponse`로 작성한다.

도메인 응답 DTO인 `{Domain}Response`와 공통 응답 래퍼인 `ApiResponse<T>`를 구분한다.

## Repository Custom 이름

QueryDSL 등으로 Repository 커스텀 구현이 필요한 경우 아래 이름을 사용한다.

- 커스텀 인터페이스: `{Domain}RepositoryCustom`
- 커스텀 구현체: `{Domain}RepositoryCustomImpl`

예시:

- `UserRepositoryCustom`
- `UserRepositoryCustomImpl`

Repository 커스텀 구현체는 Spring Data JPA 관례상 `Impl` 접미사를 허용한다.

이 규칙은 Service 구현체에서 의미 없는 `Impl` 사용을 피하는 규칙과 별도로 본다.

## Enum 이름

Enum 이름은 의미가 드러나는 명사로 작성한다.

예시:

- `UserRole`
- `OrderStatus`
- `ChatRoomStatus`

단순히 `Status`, `Type`, `Role`처럼 범위가 넓은 이름은 피한다.

도메인 의미가 드러나도록 `{Domain}{Purpose}` 형태를 우선 사용한다.

## id 필드 이름

Entity 자신의 식별자는 `id`로 작성한다.

다른 도메인이나 외부 응답에서 특정 Entity의 식별자를 표현할 때는 `{domain}Id`로 작성한다.

예시:

- User Entity 내부: `id`
- ChatRoom에서 사용자 식별자 참조: `userId`
- 응답 DTO에서 사용자 식별자 반환: `userId`

도메인 이름은 프로젝트에서 정한 이름을 따른다. 예를 들어 `user`를 쓰기로 했다면 `memberId`와 섞어 쓰지 않는다.

## 메서드/변수 이름

- camelCase를 쓴다.
- 의미를 알 수 없는 축약은 쓰지 않는다. (예: `usr` 대신 `user`)
- 조회 메서드는 Repository, Service에서 아래 기준을 우선 따른다.
  - `find...`: 결과가 없을 수 있는 조회
  - `get...`: 결과가 반드시 있어야 하는 조회
  - `exists...`: 데이터 존재 여부 확인
- boolean을 반환하는 상태 판단 메서드는 `is`, `has`, `can`으로 시작한다.
  - 예: `isExpired()`, `hasPermission()`, `canAccess()`
- 존재 여부 확인은 `exists...`를 사용하고, `is...Exists` 형태는 사용하지 않는다.
  - 예: `existsByEmail()` (O), `isEmailExists()` (X)

## 상수 이름

- UPPER_SNAKE_CASE를 쓴다. (예: `MAX_LOGIN_ATTEMPT`)

## 약어 표기

- `Id`, `Url`, `Dto`처럼 두 글자 이상 약어는 첫 글자만 대문자로 쓴다.
  - `getUserId()` (O), `getUserID()` (X)
- 이 규칙은 클래스명, 메서드명, 변수명에 모두 적용한다.