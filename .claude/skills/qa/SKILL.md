---
name: qa
description: |
  품질 검증 파이프라인: 프로덕션 코드 리뷰 → 테스트 코드 검증.
  /qa [대상 파일/패키지] [--phase review|test-review|query]
---

# QA Pipeline

품질 검증 오케스트레이터. 프로덕션 코드 리뷰와 테스트 코드 검증을 순차 실행한다.

## 사용법

```
/qa [대상 파일/패키지] [--phase review|test-review|query]
```

## 페이즈 흐름

```
review → test-review → query
```

## 옵션

- 기본 실행 (옵션 없음): review → test-review 순차 실행
- `--phase review`: 프로덕션 코드 리뷰만 실행
- `--phase test-review`: 테스트 코드 검증만 실행
- `--phase query`: 트랜잭션/쿼리 분석만 실행
- 인자 없으면 `git diff`로 현재 변경사항 전체를 대상으로 함

## 페이즈 요약

| 페이즈 | 설명 | 대상 |
|-------|------|------|
| review | 4레이어 아키텍처 기반 프로덕션 코드 리뷰 | `src/main/**` |
| test-review | 테스트 신뢰성 검증 | `src/test/**` |
| query | 트랜잭션/쿼리/영속성 컨텍스트 분석 | `@Transactional` 코드 |

## 실행 로직

1. `$ARGUMENTS`에서 `--phase` 옵션과 대상 파일/패키지를 파싱한다
2. 대상이 없으면 `git diff --name-only`로 변경 파일 목록을 가져온다
3. 대상 페이즈의 `phases/phase-<name>.md`를 읽고 실행한다
4. 기본 실행은 review → test-review를 순차 실행한다

## 심각도 분류

| 등급 | 의미 | 조치 |
|------|------|------|
| CRITICAL | 버그/데이터 손실/검증 누락 | 반드시 수정 |
| WARNING | 잠재적 문제/컨벤션 위반 | 수정 권장 |
| INFO | 개선 제안 | 선택적 적용 |
