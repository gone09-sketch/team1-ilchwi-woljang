# Package Structure

## 목적

패키지는 계층 기준이 아니라 도메인 기준으로 먼저 나눈다.

도메인 안에서 다시 계층(Controller, Service, Repository, Entity, DTO)으로 나눈다.

이 방식은 도메인이 늘어나도 한 계층 패키지에 모든 도메인의 클래스가 섞여 쌓이는 것을 막고, 한 도메인을 작업할 때 관련 파일을 한 곳에서 찾을 수 있게 해준다.

## 기본 구조

```text
com.{company}.{project}
├── domain
│   ├── user
│   │   ├── controller
│   │   ├── service
│   │   ├── repository
│   │   ├── entity
│   │   └── dto
│   │       ├── request
│   │       └── response
│   ├── chat
│   │   ├── controller
│   │   ├── service
│   │   ├── repository
│   │   ├── entity
│   │   └── dto
│   └── ...
├── common
│   ├── exception
│   ├── response
│   ├── config
│   ├── security
│   ├── entity
│   └── util
└── Application.java
```

## 도메인 패키지

- 하나의 도메인은 하나의 비즈니스 단위를 의미한다. (예: user, chat, notification, search)
- 도메인 패키지 안에서는 controller, service, repository, entity, dto를 기본 하위 구조로 사용한다.
- 도메인 패키지 이름은 단수형을 쓴다. (user, users가 아님)
- 도메인이 너무 커지면 그 안에서 다시 하위 도메인으로 나눌 수 있지만, 계층 구조(controller, service...)는 유지한다.

## common 패키지

- 두 개 이상의 도메인에서 공통으로 쓰는 코드만 common에 둔다.
- 한 도메인에만 쓰이는 예외나 응답 형식은 common이 아니라 해당 도메인 패키지 안에 둔다.
- common 하위는 역할별로 나눈다.
  - exception: 전역 예외 처리(`@ControllerAdvice` 등)
  - response: 공통 응답 포맷
  - config: Spring 설정 클래스
  - security: 인증/인가 관련 클래스
  - entity: 여러 도메인 Entity가 공통으로 상속하는 BaseEntity 등
  - util: 여러 도메인에서 재사용하는 범용 유틸

## security 패키지

- 인증/인가 관련 공통 코드는 common.security에 둔다.
- JWT, 필터, 인증 객체, 권한 검증 코드가 늘어나면 역할별 하위 패키지로 나눈다.
- 예: `security.jwt`, `security.filter`, `security.auth`, `security.annotation`

## util 패키지

- util에는 여러 도메인에서 재사용 가능한 순수 유틸만 둔다.
- 특정 도메인의 비즈니스 규칙이 들어간 유틸은 만들지 않고 해당 도메인 패키지에 둔다.
- util 클래스가 커지면 역할별로 분리한다.
- 예: `DateUtil`, `StringUtil`

## DTO 패키지 세부 구조

- dto 패키지 아래 request, response로 나눈다.
- request에는 API 요청 DTO를 둔다.
- response에는 API 응답 DTO를 둔다.
- DTO 이름 규칙은 Naming 문서를 따른다.

## 도메인 간 의존성

- 도메인 패키지끼리 서로의 entity나 repository를 직접 참조하지 않는다.
- 다른 도메인의 데이터가 필요하면 해당 도메인의 Service를 통해 가져온다.
- 다른 도메인의 상태를 변경해야 하면 해당 도메인의 Service에 명확한 비즈니스 메서드를 만들고 호출한다.
- 의존 방향이 양쪽으로 생기면(A가 B를 참조하고 B도 A를 참조) 순환 참조 위험이 있으므로, 한쪽 방향으로만 의존하도록 설계한다.

## Enum 위치

- Entity와 강하게 연결된 enum은 해당 도메인의 entity 패키지에 둔다.
- 예: `domain.user.entity.UserRole`, `domain.chat.entity.ChatRoomStatus`
- Enum이 많아져 entity 패키지가 복잡해지면 `entity.enums` 패키지로 분리할 수 있다.
- 예: `domain.user.entity.enums.UserRole`

## 고도화 예정 영역

websocket, async-concurrency, caching, indexing 같은 고도화 영역은 `CODE_CONVENTION.md`의 Planned Sections를 따른다.