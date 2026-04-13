# Git 워크플로우

## 브랜치 정리 시 안전 규칙

브랜치 삭제, `git clean` 등 정리 작업 전에 반드시 확인하라:

1. `git status`로 미커밋 변경사항(modified, untracked) 확인
2. `git stash list`로 stash 확인
3. **미커밋 변경사항이 있으면 절대 삭제하지 않는다** — 사용자에게 먼저 알리고 지시를 받을 것
4. `--force` 옵션(`git branch -D` 등) 사용 금지 — 사용자가 명시적으로 요청한 경우에만
5. PR이 머지되었더라도 워킹 디렉토리에 새 작업이 시작되었을 수 있으므로 **PR 상태만으로 판단하지 않는다**

## 커밋 규칙

**스킬 우선 사용:** `/ship --phase commit`(커밋), `/ship --phase pr`(PR) 스킬 사용을 권장한다. 단순 커밋은 git 명령어 직접 사용도 허용하되, PR 생성은 스킬을 사용하라.

**Git trailer 금지:** 커밋 메시지에 `Constraint:`, `Rejected:`, `Confidence:`, `Scope-risk:`, `Directive:`, `Not-tested:` 등 Git trailer를 추가하지 않는다. subject + body(선택)만 사용한다.

## 최신 상태 유지

**매 요청 시작 전** 아래를 순서대로 실행하라:

1. `git branch --show-current`로 현재 브랜치 확인
2. **develop이 아닌 브랜치에 있다면**:
   - PR 상태를 확인하여 merged 상태일 때만 `git checkout develop && git pull`로 복귀
   - PR이 open이거나 PR이 없으면 **브랜치를 유지** — 임의로 develop으로 돌아가지 마라
3. **develop 브랜치에 있다면**: uncommitted 변경이 없으면 `git pull --rebase --autostash` 실행
