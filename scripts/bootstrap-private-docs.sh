#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET_DIR="${ROOT_DIR}/docs/loopers"
REPO_URL="${DOCS_LOOPERS_REPO_URL:-https://github.com/riveroverflows/loopers-docs.git}"
REPO_BRANCH="${DOCS_LOOPERS_REPO_BRANCH:-main}"

if [ -e "${TARGET_DIR}" ]; then
  if git -C "${TARGET_DIR}" rev-parse --git-dir >/dev/null 2>&1; then
    echo "docs/loopers is already initialized"
    exit 0
  fi

  echo "docs/loopers exists but is not a git checkout. Move or remove it, then rerun." >&2
  exit 1
fi

git clone -b "${REPO_BRANCH}" "${REPO_URL}" "${TARGET_DIR}"
