# Phase: Plan

분석 결과를 기반으로 수정 계획을 작성하고, 리뷰 결정을 기록하는 페이즈.

## 입력

- analyze 페이즈의 분류 결과 (AGREE / TRADEOFF 확정 / ALREADY_DECIDED)

## 절차

### 1. plan.md 업데이트

AGREE 항목들을 논리적으로 그룹핑하여 CP(Checkpoint) 단위로 plan.md에 추가한다.

**그룹핑 기준:**
- 같은 파일을 수정하는 항목은 하나의 CP로 묶는다
- 관련 도메인이 같은 항목끼리 묶는다 (예: 결제 안전성, 복구 로직, 테스트 등)

**CP 번호**: 기존 plan.md의 마지막 CP 번호 다음부터 부여한다.

**형식:**
```markdown
## CP{N}. {그룹 제목}

- [ ] `{파일명}`: {수정 내용} — {근거 한 줄}
- [ ] `{파일명}`: {수정 내용}
```

### 2. docs/review-decisions.md 갱신

TRADEOFF로 최종 확정된 항목(기각 또는 부분 수용)을 `docs/review-decisions.md`에 추가한다.

**이미 같은 주제의 결정이 있으면**: `repeat_count`를 증가시키고 최신 날짜로 업데이트한다.
**새 결정이면**: 새 항목을 추가한다.

### 3. 최종 보고

```markdown
## 리뷰 반영 계획 완료

- **plan.md**: CP{N}~CP{M} 추가 ({K}건 수정 항목)
- **review-decisions.md**: {J}건 트레이드오프 기록
- **구현 준비 완료**: "진행하자"로 구현 시작
```
