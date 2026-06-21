# ANTIGRAVITY.md

## Project Rules

- 시스템/개발자/보안 지침이나 사용자 명시 요청과 충돌하면 그 지침을 우선한다.
- Antigravity는 기본적으로 read-only로 동작한다.
- 초안 작성, 리뷰, 답변은 가능하지만 실제 파일 수정, 설정 변경, git 조작은 사용자의 명시 요청이 필요하다.
- 사용자가 명시적으로 요청하지 않으면 코드 수정, 삭제, 파일 생성, 설정 변경을 하지 않는다.
- 사용자가 명시적으로 요청하지 않으면 branch 변경, commit, push, merge, rebase, PR 생성을 하지 않는다.
- Antigravity는 에이전트가 터미널, 파일, 브라우저를 직접 사용할 수 있으므로 실행 전 변경 범위와 의도를 사용자에게 명확히 공유한다.

## Repository Navigation

- 작업 시작 전 현재 브랜치와 변경 범위를 먼저 확인한다.
  - `git branch --show-current`
  - `git status --short`
  - `git diff --stat`
  - `git diff --name-only`
- 문서와 코드는 필요한 범위만 좁혀서 읽는다.
- 파일 검색은 `rg`를 우선 사용한다.
- 긴 컨벤션 문서는 한 번에 모두 읽지 않고, 필요한 `docs/conventions/*.md`만 읽는다.

## Agent Skills

- Antigravity는 `.agents/skills/*/SKILL.md`를 공용 skill 원본으로 사용한다.
- `.agents/skills`는 Codex, Gemini, Antigravity 및 다른 에이전트와 공유하는 원본 skill로 관리한다.
- `.claude/skills`는 Claude Code 자동 탐색을 위한 복제본이며 직접 수정하지 않는다.
- skill 수정은 `.agents/skills`에서만 하고, 수정 후 `scripts/sync-skills.sh`와 `scripts/check-skills.sh`를 실행한다.
- 실행 방법은 프로젝트 루트에서 아래 순서로 진행한다.
  - `cd /mnt/c/projects/team1-ilchwi-woljang`
  - `scripts/sync-skills.sh`
  - `scripts/check-skills.sh`
- 성공하면 `Synced .agents/skills -> .claude/skills`, `Agent skills are in sync.` 메시지가 출력된다.
- 실패하면 `.agents/skills`와 `.claude/skills`가 다르거나 실행 위치가 프로젝트 루트가 아닐 수 있으므로, 프로젝트 루트에서 `scripts/sync-skills.sh`를 다시 실행한 뒤 `scripts/check-skills.sh`로 재검증한다.
- 요청과 맞는 skill이 있을 때만 해당 `SKILL.md`를 읽고 적용한다.
- 현재 제공되는 skill의 용도는 다음과 같다.
  - `github-pr-write`: PR 제목/본문 초안 작성 요청에 사용한다.
  - `review`: 코드 리뷰, 백엔드 리뷰, 컨벤션 리뷰, 문서 정합성 리뷰 요청에 사용한다.
  - `test-guide`: 테스트 후보, 검증 방법, 테스트 코드 조언 요청에 사용한다.

## Review Guidance

- 리뷰 요청은 직접 수정보다 체크포인트와 검증 방법 중심으로 답변한다.
- Critical Findings는 빌드 실패, 테스트 실패, 데이터 유실, 보안 문제, API 동작 불가처럼 즉시 막아야 하는 문제에만 사용한다.
- Major는 동작 오류 가능성, 유지보수 리스크, 성능 문제처럼 수정 우선순위가 높은 항목에 사용한다.
- Minor는 코드 스타일, 네이밍, 가독성처럼 낮은 위험의 개선 항목에 사용한다.
- Suggestion은 선택적 개선이나 대안 제안에 사용한다.
- 일반 리뷰 항목은 확인할 지점, 직접 확인 방법, 관찰할 결과를 함께 제시한다.

## Implementation Guidance

- 사용자가 구현을 명시적으로 요청한 경우에만 코드를 수정한다.
- 기존 패키지 구조, 네이밍, 테스트 스타일을 우선 따른다.
- 새 의존성, 설정 변경, 구조 변경은 필요성이 명확할 때만 제안하거나 적용한다.
- 테스트가 필요한 변경이면 실행한 테스트와 실행하지 못한 테스트를 구분해서 보고한다.

## Convention Documents

- 공통 컨벤션 인덱스: `docs/CODE-CONVENTION.md`
- 세부 컨벤션: `docs/conventions/*.md`
- 작업과 직접 관련된 컨벤션 문서만 선택해서 읽는다.
