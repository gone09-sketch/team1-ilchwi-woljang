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

## 프로젝트 컨텍스트

### 기술 스택

Spring Boot 4.1.0 / Java 17 / MySQL / Spring Security (JWT)

### 빌드 & 실행

```bash
./gradlew build        # 전체 빌드
./gradlew test         # 테스트 실행
./gradlew bootRun      # 개발 서버 실행 (환경변수 필요)
```

### 환경 설정

`.env.example`을 참고해 환경변수를 설정한다. 필수 항목:
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`

### 패키지 구조

```
src/main/java/com/team1ilchwiwoljang/
├── common/        # ApiResponse, 예외처리, 설정, BaseEntity
└── domain/        # auth / member / product / category / cart / order / inquiry
```

각 도메인은 `controller / service / repository / dto / entity` 계층으로 구성된다.

### 컨벤션

계층별: `docs/conventions/*.md`
Git/PR: `docs/GIT-CONVENTION.md`, `docs/CODE-CONVENTION.md`
