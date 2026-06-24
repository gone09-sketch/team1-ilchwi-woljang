@AGENTS.md

## 빌드 & 실행

```bash
./gradlew build        # 전체 빌드
./gradlew test         # 테스트 실행
./gradlew bootRun      # 개발 서버 실행 (환경변수 필요)
```

## 환경 설정

`.env.example`을 참고해 환경변수를 설정한다. 필수 항목:

```
DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET
```

## 패키지 구조

```
src/main/java/com/team1ilchwiwoljang/
├── common/        # 공통 (ApiResponse, 예외처리, 설정, BaseEntity)
└── domain/
    ├── auth/      # 인증 (JWT)
    ├── member/    # 회원
    ├── product/   # 상품
    ├── category/  # 카테고리
    ├── cart/      # 장바구니
    ├── order/     # 주문
    └── inquiry/   # 문의
```

각 도메인은 `controller / service / repository / dto / entity` 계층으로 구성된다.

## 컨벤션

계층별 컨벤션: `docs/conventions/*.md`
Git/PR 컨벤션: `docs/GIT-CONVENTION.md`, `docs/CODE-CONVENTION.md`

## Claude Skills

사용 가능한 skills: `.claude/skills/` (github-pr-write, review, test-guide)

**Skill 수정 시 주의**: 원본은 `.agents/skills/`에서만 수정하고, 수정 후 반드시 sync 실행.

```bash
scripts/sync-skills.sh   # .agents/skills → .claude/skills 동기화
scripts/check-skills.sh  # 동기화 검증
```

## 하네스 유지보수 (진화하는 하네스)

세션 중 발견한 비자명 패턴·주의사항은 `#` 키로 즉시 이 파일에 추가한다.

새 컨벤션이나 아키텍처 결정이 생기면 `docs/conventions/`에 문서화 후 여기에 위치를 기재한다.
