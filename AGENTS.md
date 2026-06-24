# AGENTS.md

## 행동 원칙

- 에이전트는 기본적으로 read-only로 동작한다.
- 사용자가 명시적으로 요청하지 않으면 코드 수정, 삭제, 파일 생성, 설정 변경을 하지 않는다.
- 사용자가 명시적으로 요청하지 않으면 branch 변경, commit, push, merge, rebase, PR 생성을 하지 않는다.
- 시스템/개발자/보안 지침이나 사용자 명시 요청과 충돌하면 그 지침을 우선한다.

## 문서 읽기

- 문서는 `rg`, `git diff --name-only`, `git diff --stat` 등으로 범위를 좁힌 뒤 필요한 파일만 읽는다.
- 계층별 컨벤션은 `docs/conventions/<계층명>.md`만 읽는다. 목록: controller, service, repository, dto, entity, exception, validation, transaction, naming, api-response, logging, security, async-concurrency, caching, indexing, websocket, test-convention, package-structure
- 리뷰 요청은 정답 제시보다 체크포인트와 검증 방법 중심으로 답변한다.

## Skills

- `.agents/skills`는 이름과 설명을 먼저 확인하고, 요청과 맞는 `SKILL.md`만 읽어 적용한다.
- `.agents/skills`는 에이전트 공용 skill 원본이다. `.claude/skills`는 `.agents/skills` 심링크이므로 직접 수정하지 않는다.
- skill 수정은 `.agents/skills`에서만 한다.

## GitHub 워크플로

작업 순서: **이슈 생성 → 브랜치 생성 → 커밋 → PR**

### 이슈

- 이슈 생성 전에 같은 목적의 이슈가 이미 존재하는지 확인한다. 존재하면 새 이슈를 생성하지 않고 기존 이슈를 연결한다.
- 이슈 생성 시 `.github/ISSUE_TEMPLATE/`에서 적절한 템플릿을 먼저 확인한다.
- CLI로 이슈를 생성할 때는 반드시 `--assignee @me`를 포함한다. (GitHub 이슈 템플릿 `assignees` 필드는 동적 할당 미지원)

### 브랜치

- 브랜치는 이슈 번호 기반으로 생성한다.
- 형식: `타입/이슈번호-작업요약` (예: `feat/12-login-api`, `fix/18-cart-error`)
- 타입: `feat` / `fix` / `refactor` / `docs` / `test` / `chore`

### PR

- PR 생성 전에 `.github/PULL_REQUEST_TEMPLATE.md`를 반드시 읽고 해당 형식을 그대로 따른다.
- 상세 Git/PR 컨벤션은 `docs/GIT-CONVENTION.md`를 참고한다.
