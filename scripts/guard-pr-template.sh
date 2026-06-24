#!/usr/bin/env sh
set -eu

template=".github/PULL_REQUEST_TEMPLATE.md"

if [ ! -f "$template" ]; then
  echo "[hook] PR template not found: $template" >&2
  exit 2
fi

if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "[hook] PR 대상 커밋"
  git log --oneline --reverse origin/dev..HEAD
fi

echo "[hook] PR 템플릿 자동 로드 - 아래 형식을 그대로 따르세요"
echo "---"
cat "$template"

payload="$(cat)"
command_text="$(printf '%s' "$payload" | grep -o '"cmd":"[^"]*"' | sed 's/^"cmd":"//; s/"$//' || true)"

case "$command_text" in
  *"gh pr create"*|*"gh pr edit"*)
    if printf '%s' "$command_text" | grep -q -- " --draft"; then
      echo "[hook] draft PR은 허용하지 않습니다. 일반 PR로 생성하세요." >&2
      exit 2
    fi
    if printf '%s' "$command_text" | grep -q -- " --body "; then
      echo "[hook] --body 직접 입력은 셸 quoting 실수를 만들 수 있습니다. 템플릿을 파일로 작성한 뒤 --body-file을 사용하세요." >&2
      exit 2
    fi
    if ! ./gradlew test --console=plain; then
      echo "[hook] ./gradlew test가 실패했습니다. PR을 만들기 전에 테스트를 통과시켜야 합니다." >&2
      exit 2
    fi
    if ! ./gradlew spotlessCheck --console=plain; then
      echo "[hook] spotlessCheck가 실패했습니다. spotlessApply를 실행한 뒤 다시 확인합니다." >&2
      if ! ./gradlew spotlessApply --console=plain; then
        echo "[hook] spotlessApply가 실패했습니다. 포맷 문제를 수동으로 해결해야 합니다." >&2
        exit 2
      fi
      if ! ./gradlew spotlessCheck --console=plain; then
        echo "[hook] spotlessCheck가 spotlessApply 이후에도 실패했습니다." >&2
        exit 2
      fi
    fi
    ;;
esac
