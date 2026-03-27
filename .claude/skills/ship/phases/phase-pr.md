# Phase: PR

커밋 이력 기반으로 PR을 자동 생성하는 페이즈.

## 입력

`$ARGUMENTS`에서 base branch를 파싱한다. 미지정 시 `main`을 기본으로 사용.

## 사전 확인

**Step 1** (게이트 — 순차 실행):

1. `which gh`로 gh CLI 존재 확인.
   - **없으면**: "gh CLI가 설치되어 있지 않습니다. `sudo apt install gh` 또는 공식 문서를 참고해주세요." 출력 후 **즉시 종료**.

2. `gh auth status`로 인증 확인.
   - **미인증이면**: "gh 인증이 필요합니다. `gh auth login`을 실행해주세요." 안내 후 **즉시 종료**.

**Step 2**: 아래를 순차 확인:
- Git 저장소인지 확인
- `git rev-parse --abbrev-ref HEAD`로 현재 브랜치 확인 — detached HEAD이면 에러 후 종료
- `git remote get-url origin`으로 origin remote 확인 — 없으면 에러 후 종료
- 현재 브랜치가 베이스 브랜치가 아닌지 확인
- 베이스 브랜치 대비 커밋이 있는지 확인 — 없으면: "PR을 생성할 커밋이 없습니다."
- 미커밋 변경사항이 있으면 경고하고, 커밋 먼저 할지 진행할지 확인

## PR 제목 생성

```bash
git log <base-branch>..HEAD --oneline -n 50
```

- 커밋 1개: 해당 커밋 제목을 PR 제목으로 사용
- 커밋 여러 개: 전체 변경을 한국어로 요약
- 포맷: 50자 이내
- 프로젝트 규칙에 따라 `[N주차]` 접두사가 필요하면 사용자에게 확인

## PR 본문 생성

```bash
git log <base-branch>..HEAD -n 50
git diff <base-branch>...HEAD --stat
```

커밋 이력과 변경 내용을 분석하여 아래 템플릿의 각 항목을 채운다.
HTML 주석(`<!-- -->`)은 제거하고, 해당 없는 섹션은 "해당 없음"으로 표기한다.

```markdown
## 📌 Summary

- 배경:
- 목표:
- 결과:

## 🧭 Context & Decision

### 문제 정의

- 현재 동작/제약:
- 문제(또는 리스크):
- 성공 기준(완료 정의):

### 선택지와 결정

- 고려한 대안:
    - A:
    - B:
- 최종 결정:
- 트레이드오프:
- 추후 개선 여지(있다면):

## 🏗️ Design Overview

### 변경 범위

- 영향 받는 모듈/도메인:
- 신규 추가:
- 제거/대체:

### 주요 컴포넌트 책임

- `ComponentA`:
- `ComponentB`:
- `ComponentC`:

## 🔁 Flow Diagram

### Main Flow

(Mermaid 시퀀스/플로우 다이어그램)
```

## PR 생성

1. 기존 PR 확인: `gh pr view --json url` — 이미 존재하면 URL을 표시하고 선택지 제시:
   - "업데이트": push 후 기존 PR 본문을 `gh pr edit`으로 갱신
   - "신규 생성": push 후 `gh pr create`
   - "취소": 스킬 종료
2. 브랜치 푸시: `git push -u origin <branch-name>` (`timeout: 120000`)
   - push 실패 시: 에러를 표시하고 **즉시 종료**
3. PR 생성 (HEREDOC으로 body 전달):
   ```bash
   gh pr create --base <base-branch> --title "<title>" --body "$(cat <<'EOF'
   ## Background
   ...

   ## Summary
   ...

   ## Changes
   ...

   ## Review Points
   ...

   ## Checklist
   ...
   EOF
   )"
   ```
   - gh pr create 실패 시: "PR 생성에 실패했습니다. push는 완료되었으므로 수동으로 PR을 생성해주세요." 안내 후 종료.
4. PR URL을 사용자에게 표시

**금지**: `Co-Authored-By` 라인을 절대 추가하지 말 것.
**금지**: `🤖 Generated with Claude Code` 라인을 절대 추가하지 말 것.

## 다음 페이즈

PR 생성 후 `--all`이면 `handoff` 페이즈로 진행한다.
