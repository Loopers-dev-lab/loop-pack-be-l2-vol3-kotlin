# 설계 문서 업데이트 Plan

## 목적

Pragmatic Clean Architecture 도입에 따라 기존 설계 문서를 일관되게 수정한다.

## 1단계: 반드시 먼저 읽어야 할 레퍼런스 파일 (3개)

아래 세 파일이 이번 수정의 기준이다. 다른 파일보다 **먼저, 반드시** 읽는다.

1. `PRAGMATIC-CLEAN-ARCHITECTURE.md` — 아키텍처 철학 전체 정의
2. `CLAUDE.md` — 핵심 패턴, VO, Repository, Domain Service 규칙
3. `docs/architecture.md` — 레이어 구조, 설계 결정, Domain Model 표

## 2단계: 수정 대상 파일

```
docs/design/01-requirements.md
docs/design/02-sequence-diagrams.md
docs/design/03-class-diagram.md
docs/design/04-erd.md
docs/design/05-flowcharts.md
docs/note/round2-design-decisions.md
docs/note/round3-decisions.md
```

## 3단계: 수정 기준 — 핵심 변경사항

레퍼런스 파일을 읽은 후 아래 기준에 따라 각 문서를 수정한다.

### A. 용어 통일
- `Entity` → `Domain Model` (도메인 객체를 지칭할 때)
- `JPA Entity` / `XxxEntity` → `Persistence Model` 또는 `XxxEntity(JPA)` (영속성 객체를 지칭할 때)
- 두 개념이 분리되어 있음을 문서에서 명확히 구분해야 한다

### B. Domain Model 관련
- Domain Model은 순수 POJO임을 반영
- `guard()` 관련 설명은 JPA Entity 쪽으로 이동하거나 제거
- `Order.create()`는 내부에서 `OrderItem` 생성 + `totalPrice` 계산을 수행 (OrderService가 아님)
- Rich Domain Model: `Order`는 `val items: List<OrderItem>` 보유 가능

### C. Repository 구조
- `XxxJpaRepository` 별도 파일 → `XxxRepositoryImpl.kt` 내 `internal interface`로 통합
- 문서에서 "2단 위임" 언급 시 이 구조로 수정
- Domain Service는 Domain Model만 다루고, XxxEntity는 RepositoryImpl 내부에서만 사용

### D. Value Object
- 한 줄짜리 검증도 VO로 표현 가능 (`@JvmInline value class` 사용)
- 기존 "한 줄짜리는 직접 검증" 규칙 제거
- VO 선언 기준:
  - 단일 값 감싸기 → `@JvmInline value class`
  - 도메인 메서드 있음 → 일반 `class`
  - 복합 필드 → `data class`

### E. JPA 연관관계
- `@OneToMany`, `@ManyToOne`, `@ManyToMany` 전면 미사용 (Order/OrderItem만의 규칙이 아님)
- 모든 엔티티 간 관계는 Repository를 통해 명시적으로 조회

### F. DTO 변환
- Domain Service가 반환하는 것은 `Domain Model` (Entity가 아님)
- `fromDomain()` / `toDomain()` 변환은 RepositoryImpl 내부에서 처리
- Controller/Facade는 Domain Model을 받아 Dto로 변환

## 4단계: 파일별 예상 수정 범위

| 파일 | 예상 수정 범위 |
|------|--------------|
| `01-requirements.md` | 유비쿼터스 언어 표 — Entity → Domain Model 용어, VO 도입 기준 설명 |
| `02-sequence-diagrams.md` | Repository 반환 타입 (`엔티티` → `Domain Model`), Order.create() 흐름 주석 |
| `03-class-diagram.md` | 클래스 분류 (Domain Model / JPA Entity 분리 반영), VO 클래스 목록 |
| `04-erd.md` | ERD는 DB 구조 기술이므로 최소 수정. XxxEntity 기준으로 작성됨을 명시 |
| `05-flowcharts.md` | 플로우차트 내 객체 용어 통일 |
| `round2-design-decisions.md` | VO 결정, Entity 패턴 결정 부분 업데이트 |
| `round3-decisions.md` | Pragmatic Clean Architecture 도입 결정 및 근거 추가 |

## 주의사항

- **수술적 변경**: 요청된 부분만 수정. 흐름/로직/비즈니스 규칙은 건드리지 않는다.
- **불확실하면 멈추고 질문**: 해석이 여러 개 가능한 경우 선택지를 제시한다.
- **레퍼런스 3개 파일이 기준**: 문서 간 충돌 시 레퍼런스 파일이 우선한다.
- ERD(`04-erd.md`)는 DB 물리 구조이므로 JPA Entity 관점에서 작성된 게 맞다. 무리하게 바꾸지 않는다.
