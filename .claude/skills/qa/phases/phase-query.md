# Phase: Query

Spring @Transactional, JPA, QueryDSL 기반 코드의 트랜잭션 범위, 영속성 컨텍스트, 쿼리 실행 시점을 분석하는 페이즈.

분석 대상: $ARGUMENTS

## 반드시 먼저 읽을 파일

- `CLAUDE.md` (프로젝트 루트)
- `apps/commerce-api/CLAUDE.md`
- `apps/commerce-api/src/main/kotlin/com/loopers/application/CLAUDE.md`
- `apps/commerce-api/src/main/kotlin/com/loopers/domain/CLAUDE.md`
- `apps/commerce-api/src/main/kotlin/com/loopers/infrastructure/CLAUDE.md`

## Analysis Scope

이 페이즈는 아래 대상에 대해 분석한다.
- `@Transactional`이 선언된 클래스 / 메서드
- Application Layer (UseCase) 코드
- JPA Entity, Repository, QueryDSL 사용 코드
- 하나의 유즈케이스(요청 흐름) 단위

> 컨트롤러 → UseCase → Repository 전체 흐름을 기준으로 분석하며, 특정 메서드만 떼어내어 판단하지 않는다.

## 절차

### 1. 대상 코드 탐색

1. `$ARGUMENTS`로 지정된 대상을 찾는다 (UseCase명, Controller명, 도메인명 등)
2. 해당 요청 흐름의 전체 코드를 읽는다: Controller → UseCase → Domain Service → Repository
3. `@Transactional` 선언 위치와 속성(`readOnly`, `propagation` 등)을 파악한다

### 2. Transaction Boundary 분석

다음을 순서대로 확인한다.
- 트랜잭션 시작 지점은 어디인가? (UseCase / Domain Service / 그 외)
- 트랜잭션이 실제로 필요한 작업은 무엇인가? (상태 변경 / 단순 조회)
- 트랜잭션 내부에서 수행되는 작업 나열 (외부 API 호출, QueryDSL 조회, 반복문 처리 등)

**출력 형식:**
```
- 현재 트랜잭션 범위:
  UseCase.method()
    ├─ [작업 1]
    ├─ [작업 2]
    ├─ [작업 3]
    └─ [작업 4]

- 트랜잭션이 필요한 핵심 작업:
  - [작업 A]
  - [작업 B]
```

### 3. 불필요하게 큰 트랜잭션 식별

아래 패턴이 존재하는지 점검한다.
- Controller에서 `@Transactional`이 사용되고 있음
- 읽기 전용 로직이 쓰기 트랜잭션에 포함됨
- 외부 시스템 호출이 트랜잭션 내부에 포함됨
- 트랜잭션 내부에서 대량 조회 / 복잡한 QueryDSL 실행
- 상태 변경 이후에도 트랜잭션이 길게 유지됨

각 패턴에 대해 **해당 여부**와 **근거 코드 위치**(파일:라인)를 명시한다.

### 4. JPA / 영속성 컨텍스트 관점 분석

다음을 중심으로 분석한다.
- Entity 변경이 언제 flush 되는지
- 조회용 Entity가 변경 감지 대상이 되는지
- 지연 로딩으로 인해 트랜잭션 후반에 쿼리가 발생할 가능성
- `@Transactional(readOnly = true)` 미적용 여부

**체크리스트:**
- [ ] 단순 조회인데 Entity 반환 후 변경 가능성 존재?
- [ ] DTO Projection 대신 Entity 조회 사용 여부
- [ ] QueryDSL 조회 결과가 영속성 컨텍스트에 포함되는지
- [ ] 비관적 락 사용 시 락 범위가 적절한지

### 5. Improvement Proposal (선택적 제안)

개선안은 강제하지 않고 선택지로 제시한다. 각 제안에 **trade-off**를 반드시 함께 명시한다.

가능한 개선 방향:
- 트랜잭션 분리 (조회 → 쓰기 분리)
- `@Transactional(readOnly = true)` 적용
- DTO Projection (읽기 전용 모델) 도입
- 외부 호출 / 이벤트 발행을 트랜잭션 외부로 이동
- Application Service / Domain Service 책임 재조정

**출력 형식:**
```
[개선안 N]
- 내용: ...
- 기대 효과: ...
- Trade-off: ...
- 적용 난이도: 낮음/중간/높음
```

## 보고 형식

```markdown
# 트랜잭션 분석: [대상명]

## 1. Transaction Boundary
(트랜잭션 범위 트리)

## 2. 불필요하게 큰 트랜잭션
(패턴별 해당 여부 + 근거)

## 3. JPA / 영속성 컨텍스트
(체크리스트 결과)

## 4. 종합 판단
- 현재 구조의 의도: ...
- 주요 리스크: ...
- 개선 우선순위: ...

## 5. 개선 제안 (선택적)
(제안이 있는 경우만)
```

## 규칙

- 코드를 수정하지 않는다. 분석과 제안만 수행한다
- 파일 경로와 라인 번호를 정확히 참조한다
- 현재 구조의 의도를 먼저 파악하고, 그 위에서 개선점을 제시한다
- "이렇게 해야 한다"가 아니라 "이런 선택지가 있고, trade-off는 이렇다"로 제시한다
