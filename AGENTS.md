# AGENTS.md

## Project Rules

- Codex는 기본적으로 read-only로 동작한다.
- 사용자가 명시적으로 요청하지 않으면 코드 수정, 삭제, 파일 생성, 설정 변경을 하지 않는다.
- 사용자가 명시적으로 요청하지 않으면 branch 변경, commit, push, merge, rebase, PR 생성을 하지 않는다.
- `.agents/skills`는 이름과 설명을 먼저 확인하고, 요청과 맞는 `SKILL.md`만 읽어 적용한다.
- `.agents/skills`는 에이전트 공용 skill 원본이며, `.claude/skills`는 Claude Code 자동 탐색을 위한 복제본이다.
- skill 수정은 `.agents/skills`에서만 하고, 수정 후 `scripts/sync-skills.sh`와 `scripts/check-skills.sh`를 실행한다.
- 실행 방법은 프로젝트 루트에서 아래 순서로 진행한다.
  - `cd /mnt/c/projects/team1-ilchwi-woljang`
  - `scripts/sync-skills.sh`
  - `scripts/check-skills.sh`
- 성공하면 `Synced .agents/skills -> .claude/skills`, `Agent skills are in sync.` 메시지가 출력된다.
- 실패하면 `.agents/skills`와 `.claude/skills`가 다르거나 실행 위치가 프로젝트 루트가 아닐 수 있으므로, 프로젝트 루트에서 `scripts/sync-skills.sh`를 다시 실행한 뒤 `scripts/check-skills.sh`로 재검증한다.
- 문서는 `rg`, `git diff --name-only`, `git diff --stat` 등으로 범위를 좁힌 뒤 필요한 파일만 읽는다.
- 긴 컨벤션 문서는 한 번에 읽지 않고, 필요한 `docs/conventions/*.md`만 읽는다.
- 리뷰 요청은 정답 제시보다 체크포인트와 검증 방법 중심으로 답변한다.
- 시스템/개발자/보안 지침이나 사용자 명시 요청과 충돌하면 그 지침을 우선한다.
