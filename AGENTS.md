# AGENTS.md

## Project Rules

- 에이전트는 기본적으로 read-only로 동작한다.
- 사용자가 명시적으로 요청하지 않으면 코드 수정, 삭제, 파일 생성, 설정 변경을 하지 않는다.
- 사용자가 명시적으로 요청하지 않으면 branch 변경, commit, push, merge, rebase, PR 생성을 하지 않는다.
- `.agents/skills`는 이름과 설명을 먼저 확인하고, 요청과 맞는 `SKILL.md`만 읽어 적용한다.
- `.agents/skills`는 에이전트 공용 skill 원본이다. `.claude/skills`는 `.agents/skills` 심링크이므로 직접 수정하지 않는다.
- skill 수정은 `.agents/skills`에서만 한다.
- 문서는 `rg`, `git diff --name-only`, `git diff --stat` 등으로 범위를 좁힌 뒤 필요한 파일만 읽는다.
- 긴 컨벤션 문서는 한 번에 읽지 않고, 필요한 `docs/conventions/*.md`만 읽는다.
- 리뷰 요청은 정답 제시보다 체크포인트와 검증 방법 중심으로 답변한다.
- 시스템/개발자/보안 지침이나 사용자 명시 요청과 충돌하면 그 지침을 우선한다.
- 이슈(Issue)를 다룰 때에는 기존에 같은 목적의 이슈가 이미 존재하는지 먼저 확인하고, 존재한다면 새 이슈를 생성하지 않고 기존 이슈를 연결하여 사용한다.
- PR 생성 전에 `.github/PULL_REQUEST_TEMPLATE.md`를 반드시 읽고 해당 형식을 그대로 따른다.

