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

커밋 메시지와 diff 통계를 분석하여 본문 작성.

```
## Background
이 변경이 필요한 배경을 설명한다. 어떤 문제가 있었는지, 비즈니스 맥락은 무엇인지를
리뷰어가 코드를 읽기 전에 이해할 수 있도록 자연스러운 문장으로 서술한다.

## Summary
이 PR에서 무엇을 했는지 요약한다. 핵심 접근 방식과 설계 판단을
간결한 문장으로 설명한다. 리뷰어가 diff를 열기 전에 전체 그림을 잡을 수 있어야 한다.

## Changes
구체적으로 무엇이 바뀌었는지를 기능 단위로 설명한다. 파일 단위가 아니라
"무엇을 왜 그렇게 바꿨는지"를 문장으로 풀어쓴다.

## Review Points
리뷰어에게 특별히 봐줬으면 하는 포인트를 나열한다.
설계 결정의 트레이드오프, 고민했던 대안, 피드백이 필요한 부분 등.

## Checklist
- [ ] 주요 기능이 로컬에서 정상 동작하는지 확인
- [ ] 기존 테스트가 통과하는지 확인
- [ ] (해당 시) 새로운 테스트를 추가했는지 확인
```

**작성 규칙**:
1. **문장형 서술**: 모든 섹션은 `-` bullet이 아닌 자연스러운 한국어 문장으로 쓴다. 단, Checklist는 체크박스 형태.
2. **Background != Summary**: Background는 "왜(문제/맥락)", Summary는 "무엇을(해결책)". 둘을 혼합하지 않는다.
3. **Changes는 기능 단위**: 파일명 나열이 아니라 기능 관점에서 "무엇이 어떻게 바뀌었고, 왜 그 방식을 선택했는지".
4. **Review Points 필수**: CLAUDE.md의 "리뷰 포인트 필수 작성" 규칙 준수.
5. **Checklist는 동적 생성**: 변경 내용에 따라 항목을 조정한다.

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
