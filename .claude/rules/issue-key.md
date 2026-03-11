# 이슈 키 규칙

이슈 키 관련 공통 규칙. ship 스킬(phase-commit, phase-pr)에서 참조한다.

## 값 참조

이슈 키 정규식 등 구체적인 값은 `.claude/config.json`의 `issueKey` 필드를 참조한다.

## 파싱

- `git branch --show-current`로 브랜치명을 확인한다.
- 브랜치명에서 이슈 키 패턴(`config.json` → `issueKey.pattern`)을 추출한다.
- 예시: `feat/JIRA-123/login` → `JIRA-123`, `feature/AFS-6/local-ddl` → `AFS-6`

## 미발견 시 처리

브랜치명에 이슈 키가 없으면:

1. AskUserQuestion으로 이슈 키 입력을 요청한다.
2. 선택지에 "건너뛰기 (이슈 키 없이 진행)" 옵션을 포함한다.
3. 사용자가 **명시적으로 "건너뛰기"를 선택**한 경우에만 이슈 키 없이 진행한다.
4. **빈 응답이나 선택 없음은 건너뛰기가 아니다** — 재질문한다.
