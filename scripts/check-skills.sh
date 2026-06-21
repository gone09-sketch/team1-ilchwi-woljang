#!/usr/bin/env sh

set -eu

SOURCE_DIR=".agents/skills"
CLAUDE_DIR=".claude/skills"

if [ ! -d "$SOURCE_DIR" ]; then
  echo "Missing source skill directory: $SOURCE_DIR" >&2
  exit 1
fi

if [ ! -d "$CLAUDE_DIR" ]; then
  echo "Missing Claude skill directory: $CLAUDE_DIR" >&2
  echo "Run scripts/sync-skills.sh to create it." >&2
  exit 1
fi

if diff -qr "$SOURCE_DIR" "$CLAUDE_DIR" >/dev/null; then
  echo "Agent skills are in sync."
  exit 0
fi

echo "Agent skills are out of sync." >&2
echo "Run scripts/sync-skills.sh, then re-run this check." >&2
diff -qr "$SOURCE_DIR" "$CLAUDE_DIR" >&2
exit 1
