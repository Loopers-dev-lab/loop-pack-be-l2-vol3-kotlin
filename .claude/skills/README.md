# Claude Code Skills

TDD (Red → Green → Refactor) + Tidy First + 설계 워크플로우에 맞춘 슬래시 스킬 모음.

---

## 작업 흐름

일반적인 기능 개발은 아래 순서를 따른다. 모든 단계가 매번 필요하지는 않고, 상황에 맞게 건너뛸 수 있다.

### 1단계: 설계 (`/design`)

요구사항이 생기면 가장 먼저 설계부터 시작한다. `/design`은 5개 Phase를 순서대로 실행하는 파이프라인이다.

```
/design 쿠폰 발급 기능 요구사항

  Phase 1. requirements  — 요구사항 분석 + 개발자와 Q&A로 모호한 부분 해소
  Phase 2. sequence      — 시퀀스 다이어그램 (Mermaid)
  Phase 3. class         — 클래스 다이어그램 (Mermaid)
  Phase 4. erd           — ERD (Mermaid)
  Phase 5. review        — 전체 설계 문서 리뷰 + 코드와의 정합성 검증
```

각 Phase 완료 후 "다음으로 갈까요?"를 묻는다. 수정을 요청하면 현재 Phase를 다시 실행한다.
산출물은 `docs/design/` 아래에 번호순으로 저장된다 (`01-requirements.md`, `02-sequence-diagrams.md`, ...).

**옵션:**

- `--phase erd` — ERD만 단독 실행
- `--quick` — 다이어그램 3개(sequence/class/erd)를 건너뛰고 요구사항 → 리뷰만 실행
- `--resume` — 중단된 지점에서 재개 (`.design/state.md`에 진행 상태가 저장됨)
- `--status` — 현재 진행 상황만 확인

**이전 라운드 설계가 있는 상태에서 새 요구사항이 추가되면?**
`/design 새 요구사항` 으로 다시 실행하면 된다. requirements Phase에서 기존 문서와 새 제약사항을 비교 분석한다.

### 2단계: 구현 계획 (`/plan`)

설계 문서가 나왔으면, 구현 항목을 TDD 단위로 분해한 plan.md를 작성한다.

```
/plan 쿠폰 발급 기능
```

plan.md에는 아래 형식으로 체크리스트가 만들어진다:

```
- [ ] [RED] CouponRepository Fake 작성
- [ ] [RED] 쿠폰 발급 성공 테스트
- [ ] [GREEN] IssueCouponUseCase 구현
- [ ] [RED] 수량 초과 발급 실패 테스트
- [ ] [GREEN] 재고 검증 로직 구현
- ...
── checkpoint: lint + test ──
```

### 3단계: TDD 구현 (`/tdd`)

**A. 수동 — 한 사이클씩 직접 진행**

```
/tdd 쿠폰 발급 성공 케이스

  [Red]      실패하는 테스트 작성 → 실패 확인
  [Green]    테스트 통과시키는 최소 구현
  [Refactor] 코드 정리 (테스트는 여전히 통과)
```

한 사이클이 끝나면 다음 요구사항으로 반복할지 물어본다.
특정 Phase만 실행하고 싶으면 `--phase red`, `--phase green`, `--phase refactor`를 쓴다.

버그 수정은 `--fix`를 붙이면 된다: 재현 테스트(Red) → 최소 수정(Green), Refactor는 생략.

### 4단계: 출시 (`/ship`)

```
/ship                   → 커밋만 (기본)
/ship --phase pr        → 커밋 + PR
/ship --all             → 커밋 + PR + 핸드오프
/ship --phase handoff   → 핸드오프 노트만
```

커밋 전에 자동으로 다음을 수행한다:

1. 프로젝트 린트 도구 실행 (포맷 + 검증)
2. 프로젝트 테스트 스위트 실행
3. Tidy First 규칙에 따라 구조적 변경과 행위적 변경을 분리 커밋

`--phase pr`이면 커밋 후 PR까지, `--all`이면 핸드오프 노트까지 이어서 실행한다.

---

## 스킬 목록

### Multi-Phase 스킬 (파이프라인)

| 스킬        | 설명              | Phase 구성                                       |
|-----------|-----------------|------------------------------------------------|
| `/design` | 설계 사이클 전체 실행    | requirements → sequence → class → erd → review |
| `/tdd`    | TDD 사이클 실행      | red → green → refactor                         |
| `/ship`   | 출시 워크플로우        | commit → pr → handoff                          |
| `/qa`     | 품질 검증 파이프라인     | review → test-review (+query)                  |

### 개별 스킬

| 스킬           | 설명                      | 인자            |
|--------------|-------------------------|---------------|
| `/plan`           | plan.md 작성/업데이트         | 요구사항/PRD (선택) |
| `/humanizer`      | AI 글 교정 (audit/rewrite) | 텍스트 또는 파일 경로  |
| `/sync-config`    | config repo에 변경사항 동기화  | -               |
| `/resume-context` | 이전 세션 컨텍스트 복원        | -               |

---

## 관련 문서

- 요구사항: `docs/requirements/` (라운드별 분리)
- 설계 산출물: `docs/design/`
- 구현 계획: `plan.md` (프로젝트 루트)
