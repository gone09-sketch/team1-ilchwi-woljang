#!/usr/bin/env sh

set -eu

SOURCE_DIR=".agents/skills"
CLAUDE_DIR=".claude/skills"

if [ ! -d "$SOURCE_DIR" ]; then
  echo "Missing source skill directory: $SOURCE_DIR" >&2
  exit 1
fi

rm -rf "$CLAUDE_DIR"
mkdir -p "$CLAUDE_DIR"
cp -R "$SOURCE_DIR"/. "$CLAUDE_DIR"/

echo "Synced $SOURCE_DIR -> $CLAUDE_DIR"
