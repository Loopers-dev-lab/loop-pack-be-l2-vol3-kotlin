#!/usr/bin/env bash
set -uo pipefail
INPUT=$(cat /dev/stdin 2>/dev/null || echo '{}')
case "$INPUT" in
  *git*commit*)
    CURRENT_BRANCH=$(git symbolic-ref --short HEAD 2>/dev/null || echo "")
    if [ -z "$CURRENT_BRANCH" ]; then
      # -C "path" 또는 -C path 형식 지원
      GIT_DIR=$(echo "$INPUT" | sed -n 's/.*git[[:space:]]\{1,\}-C[[:space:]]\{1,\}"\{0,1\}\([^"[:space:]]\{1,\}\)"\{0,1\}.*/\1/p' 2>/dev/null || echo "")
      # --git-dir 형식 지원
      if [ -z "$GIT_DIR" ]; then
        GIT_DIR=$(echo "$INPUT" | sed -n 's/.*git[[:space:]]\{1,\}--git-dir[=[:space:]]\{1,\}"\{0,1\}\([^"[:space:]]\{1,\}\)"\{0,1\}.*/\1/p' 2>/dev/null || echo "")
      fi
      if [ -n "$GIT_DIR" ] && [ -d "$GIT_DIR" ]; then
        CURRENT_BRANCH=$(git -C "$GIT_DIR" symbolic-ref --short HEAD 2>/dev/null || echo "")
      fi
    fi
    if [[ "$CURRENT_BRANCH" =~ ^(develop|main|master)$ ]]; then
      printf '{\n  "hookSpecificOutput": {\n    "hookEventName": "PreToolUse",\n    "permissionDecision": "deny",\n    "permissionDecisionReason": "%s 브랜치에서는 커밋할 수 없습니다. 작업 브랜치를 먼저 생성하세요."\n  }\n}\n' "$CURRENT_BRANCH"
      exit 0
    fi
    ;;
esac
exit 0
