# team1-ilchwi-woljang

팀 프로젝트 초기 Spring Boot 백엔드 뼈대입니다.

아직 서비스 도메인은 확정되지 않았으며, 공통 개발 환경과 컨벤션, 에이전트 사용 기준을 먼저 맞추는 단계입니다.

## Tech Stack

- Java 17
- Spring Boot 4.1.0
- Gradle
- Spring MVC
- Spring Data JPA
- Spring Security
- Bean Validation
- JWT: JJWT 0.13.0
- MySQL
- H2: test runtime only
- Lombok

## Local Setup

### JDK

프로젝트 기준 Java 버전은 17입니다.

`build.gradle`에도 Java 17 toolchain이 설정되어 있습니다.

```gradle
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
```

IntelliJ에서는 아래 설정을 확인합니다.

- `File > Project Structure > Project`
  - SDK: JDK 17
  - Language level: 17
- `File > Project Structure > Modules`
  - Module SDK: Project SDK 또는 JDK 17
- `Settings > Build, Execution, Deployment > Build Tools > Gradle`
  - Gradle JVM: Project SDK 또는 JDK 17
  - Build and run using: Gradle
  - Run tests using: Gradle

### Line Endings

저장소의 텍스트 파일은 LF 기준으로 관리합니다.

Windows 사용자는 아래 설정을 권장합니다.

```bash
git config --global core.autocrlf false
```

IntelliJ에서는 아래 설정을 확인합니다.

```text
Settings > Editor > Code Style > Line separator: Unix and macOS (\n)
```

줄바꿈 정책은 `.gitattributes`를 따릅니다.

## Gradle Commands

프로젝트 루트에서 실행합니다.

```bash
./gradlew test
```

의존성 확인:

```bash
./gradlew dependencies --configuration runtimeClasspath
./gradlew dependencies --configuration testRuntimeClasspath
```

## Project Rules

- 사용자가 명시적으로 요청하지 않으면 코드 수정, 삭제, 파일 생성, 설정 변경을 하지 않습니다.
- 사용자가 명시적으로 요청하지 않으면 branch 변경, commit, push, merge, rebase, PR 생성을 하지 않습니다.
- 문서는 필요한 범위만 좁혀서 읽습니다.
- 긴 컨벤션 문서는 한 번에 모두 읽지 않고, 작업과 직접 관련된 `docs/conventions/*.md`만 읽습니다.
- 리뷰 요청은 정답 제시보다 체크포인트와 검증 방법 중심으로 답변합니다.

## Convention Documents

컨벤션 인덱스:

```text
docs/CODE-CONVENTION.md
```

세부 컨벤션:

```text
docs/conventions/
```

주요 문서:

- Package Structure
- Naming
- DTO
- Entity
- Controller
- Service
- Repository
- Exception
- API Response
- Validation
- Transaction
- Security
- Logging
- Test Convention

## Agent Setup

프로젝트에서 사용하는 에이전트 지침 파일은 다음과 같습니다.

```text
AGENTS.md        # Codex
CLAUDE.md        # Claude Code
GEMINI.md        # Gemini
ANTIGRAVITY.md   # Antigravity
```

공용 skill 원본은 아래 경로입니다.

```text
.agents/skills/
```

현재 제공 skill:

- `github-pr-write`: PR 제목/본문 초안 작성
- `review`: 코드 리뷰, 백엔드 리뷰, 컨벤션 리뷰, 문서 정합성 리뷰
- `test-guide`: 테스트 후보와 검증 방법 제안

Claude Code는 네이티브 skill 자동 탐색을 위해 아래 복제본을 사용합니다.

```text
.claude/skills/
```

`.claude/skills`는 직접 수정하지 않습니다.

## Skill Sync

skill은 반드시 `.agents/skills`에서만 수정합니다.

수정 후 프로젝트 루트에서 아래 명령을 순서대로 실행합니다.

```bash
scripts/sync-skills.sh
scripts/check-skills.sh
```

성공하면 아래 메시지가 출력됩니다.

```text
Synced .agents/skills -> .claude/skills
Agent skills are in sync.
```

실패하면 실행 위치가 프로젝트 루트인지 확인하고, 다시 동기화 후 검증합니다.

```bash
scripts/sync-skills.sh
scripts/check-skills.sh
```

## Git Check

작업 전후로 변경 상태를 확인합니다.

```bash
git status --short
```

줄바꿈 정책 확인이 필요하면 아래 명령을 사용할 수 있습니다.

```bash
git ls-files --eol
```
