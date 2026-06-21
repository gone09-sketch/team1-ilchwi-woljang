# 코드 컨벤션

이 문서는 코드 컨벤션의 인덱스다.

전체 컨벤션을 한 번에 읽지 않고, 작업과 직접 관련된 섹션 파일만 읽는다.

## Sections

- [Package Structure](conventions/package-structure.md)
- [Naming](conventions/naming.md)
- [DTO](conventions/dto.md)
- [Entity](conventions/entity.md)
- [Controller](conventions/controller.md)
- [Service](conventions/service.md)
- [Repository](conventions/repository.md)
- [Exception](conventions/exception.md)
- [API Response](conventions/api-response.md)
- [Validation](conventions/validation.md)
- [Transaction](conventions/transaction.md)
- [Security](conventions/security.md)
- [Logging](conventions/logging.md)

## Planned Sections

아래 섹션은 프로젝트 고도화 단계에서 필요할 때 작성한다.

- [WebSocket](conventions/websocket.md)
- [Async & Concurrency](conventions/async-concurrency.md)
- [Caching](conventions/caching.md)
- [Indexing](conventions/indexing.md)

## Test Convention

테스트 컨벤션은 코드 컨벤션 본문에서 관리하지 않는다.

테스트 작성, 수정, 리뷰 요청은 아래 문서와 프로젝트 테스트 스킬을 따른다.

- [Test Convention](conventions/test-convention.md)

## Reading Rule

- 전체 컨벤션을 한 번에 읽지 않는다.
- 작업 대상과 직접 관련된 섹션 파일만 읽는다.
- 여러 계층을 함께 수정하는 경우 필요한 섹션 파일만 조합해서 읽는다.
- Planned Sections는 해당 주제를 작업할 때 파일이 없으면 먼저 작성하고, 파일이 있으면 그 기준을 따른다.
- 테스트 작성/수정 요청은 `conventions/test-convention.md`와 프로젝트 테스트 스킬을 따른다.