# AGENTS.md

## 이 파일에 대해

- 이 파일이 모든 에이전트의 단일 정본이다.
- `CLAUDE.md`는 `@AGENTS.md` 포인터만 두고 내용을 복제하지 않는다.
- 이 파일이 한 페이지를 넘게 비대해지면 그때 섹션을 별도 파일로 분리한다. 그 전엔 한 장으로 유지한다.

## 에이전트 주의사항

반복적으로 발생한 실수에서 증류한 규칙. 위반 시 즉시 되돌린다.

- 코드로 알 수 있는 정보(빌드 명령어, 패키지 구조, 기술 스택, 환경변수)를 이 파일에 추가하지 않는다.
- AGENTS.md 내용을 CLAUDE.md에 복제하지 않는다. CLAUDE.md는 `@AGENTS.md` 포인터만 둔다.
- skill 파일의 파일 경로 참조는 반드시 실제 `ls`로 존재 여부를 확인한 뒤 작성한다.
- 확인하지 않은 테스트·검증 항목을 완료로 표시하지 않는다.
- PR을 마무리할 때는 `./gradlew test`와 `./gradlew spotlessCheck`를 실제로 통과시킨 뒤에만 완료로 판단한다.
- `spotlessCheck`가 실패하면 `./gradlew spotlessApply`를 먼저 실행하고, 다시 `spotlessCheck`로 재확인한다.
- PR 작성 요청에 응답할 때는 포함 커밋 ID를 함께 명시한다.
- PR을 올릴 때는 draft로 만들지 않는다.
- 도구 출력이 압축되거나 일부만 표시된 파일은 읽은 것으로 간주하지 않는다. 필요한 범위를 줄 단위로 다시 읽고 확인한 뒤 진행한다.
- PR 생성·수정 시 템플릿을 추정해서 작성하지 않는다. `.github/PULL_REQUEST_TEMPLATE.md`의 모든 섹션을 확인하고 1:1로 맞춘다.
- PR을 올릴 때는 draft로 만들지 않는다.
- PR 생성 전에 기준 브랜치 대비 커밋 ID 목록을 확인한다.
- `gh pr create/edit`에서 긴 본문을 `--body` 문자열로 직접 넘기지 않는다. 셸이 백틱·따옴표를 해석할 수 있으므로 본문 파일을 만들고 `--body-file`을 사용한다.
- `git branch`, `git switch` 등에서 `.git/*.lock` 생성 실패나 `Operation not permitted`가 발생하면 다른 브랜치/PR 전략으로 우회하지 않는다. 원래 의도한 git 작업을 권한 문제로 판단해 같은 명령을 승인받아 재시도하거나 멈추고 보고한다.

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
