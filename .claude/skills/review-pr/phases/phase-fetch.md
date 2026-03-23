# Phase: Fetch

PR에서 AI 리뷰어(CodeRabbit, Gemini)의 리뷰를 수집하는 페이즈.

## 입력

- `$PR_NUMBER`: PR 번호 (SKILL.md에서 파싱됨)
- `$AFTER_COMMIT`: (선택) 이 커밋 이후의 리뷰만 필터링

## 절차

### 1. 리포지토리 정보 추출

```bash
gh repo view --json nameWithOwner -q '.nameWithOwner'
```

결과에서 `{owner}/{repo}` 를 추출한다.

### 2. 리뷰 수집

두 종류의 GitHub API에서 AI 리뷰를 수집한다:

**Issue Comments** (PR 본문 아래 일반 코멘트):
```bash
gh api repos/{owner}/{repo}/issues/{pr_number}/comments --paginate
```

**Review Comments** (코드 라인에 달린 인라인 코멘트):
```bash
gh api repos/{owner}/{repo}/pulls/{pr_number}/comments --paginate
```

**Reviews** (리뷰 요약):
```bash
gh api repos/{owner}/{repo}/pulls/{pr_number}/reviews --paginate
```

### 3. AI 리뷰어 필터링

`user.login`이 아래에 해당하는 코멘트만 추출:
- `coderabbitai`
- `gemini-code-assist`

### 4. 커밋 기준 필터링 (`--after` 옵션)

`$AFTER_COMMIT`이 주어진 경우:

1. 해당 커밋의 시각을 구한다:
   ```bash
   git log -1 --format='%aI' {commit_hash}
   ```
2. `created_at` 또는 `submitted_at`이 이 시각 이후인 리뷰만 포함한다.

`$AFTER_COMMIT`이 없으면 전체 리뷰를 수집한다.

### 5. 파싱

JSON을 python3으로 파싱한다 (jq 미설치 환경 대응):

```python
import json, sys
data = json.load(sys.stdin)
# 필터링 로직
```

### 6. 출력 형식

수집된 리뷰를 아래 구조로 정리하여 다음 페이즈에 전달:

```
=== [리뷰어] [타입: review|comment|inline] [날짜] ===
파일: {path} (있으면)
라인: {line} (있으면)
---
{본문}
===END===
```

### 7. 수집 결과 요약 보고

```
## 리뷰 수집 결과
- CodeRabbit: N건 (review M건, comment K건)
- Gemini: N건 (review M건, comment K건)
- 필터: {after_commit} 이후 / 전체
```
