---
name: review-pr
description: |
  PR에 달린 AI 리뷰(CodeRabbit, Gemini)를 수집·분석·계획하는 파이프라인.
  /review-pr <PR_URL_or_NUMBER> [--after <commit>] [--phase fetch|analyze|plan]
---

# Review PR Pipeline

AI 리뷰어(CodeRabbit, Gemini Code Assist)가 남긴 리뷰를 자동 수집하고,
하나도 빠짐없이 검토한 후, 수정 계획을 세우는 오케스트레이터.

## 사용법

```
/review-pr <PR_URL_or_NUMBER> [--after <commit_hash>] [--phase fetch|analyze|plan]
```

## 페이즈 흐름

```
fetch → analyze → plan
```

## 옵션

- 기본 실행 (옵션 없음): fetch → analyze → plan 순차 실행
- `--phase fetch`: 리뷰 수집만 실행
- `--phase analyze`: 수집된 리뷰를 분석·분류만 실행
- `--phase plan`: 수정 계획 작성만 실행
- `--after <commit_hash>`: 해당 커밋 이후에 작성된 리뷰만 필터링

## 인자 파싱

- `$ARGUMENTS`에서 PR 번호 또는 URL을 추출한다
  - URL 형식: `https://github.com/{owner}/{repo}/pull/{number}` → number 추출
  - 숫자만: PR 번호로 사용
- `--after` 뒤의 값은 git commit hash (short/full 모두 허용)
- `--phase` 뒤의 값은 실행할 페이즈

## 산출물

| 페이즈 | 산출물 |
|--------|--------|
| fetch | 리뷰 데이터 (메모리에 보관, 다음 페이즈로 전달) |
| analyze | `review-summary.md` 작성/갱신 + 사용자에게 분류 결과 보고 (AGREE / TRADEOFF / DISMISS) |
| plan | `plan.md` 업데이트 + `docs/review-decisions.md` 갱신 |

## 참조 파일

- `docs/review-decisions.md`: 이전에 결정된 트레이드오프 기록. analyze 시 자동 참조하여 반복 지적을 식별
- `review-summary.md`: PR별 리뷰 검토 결과 문서 (프로젝트 루트)
- `plan.md`: 수정 계획 체크리스트 (프로젝트 루트)
