# Loopers Commerce Project

Kotlin + Spring Boot 기반 e-commerce 백엔드 프로젝트입니다.
**모든 기술적 선택에는 "왜?"가 있어야 합니다.**

## 1. 프로젝트 개요

- **도메인**: 유저(회원가입/조회/비밀번호 변경), 주문, 상품, 결제 등으로 확장 예정
- **목표**: 테스트 가능한 구조(TDD, 테스트 피라미드)를 유지하며 단계적으로 기능 확장
- **스택**: Kotlin 2.0.20 · Java 21 · Spring Boot 3.4.4 · MySQL 8.0 · Redis 7.0

## 2. 빌드 / 실행 / 테스트

```bash
./gradlew clean build                    # 빌드
./gradlew :apps:commerce-api:bootRun     # API 서버 실행
./gradlew test                           # 전체 테스트
./gradlew ktlintCheck                    # 코드 스타일 검사
```

코드 변경을 제안할 때, 어떤 테스트(Unit/Integration/E2E)를 어떻게 돌려야 하는지 함께 설명해 주세요.

## 3. 레이어 / 패키지 구조 (공통 원칙)

```
Interfaces → Application (UseCase) → Domain ← Infrastructure
```

- **Interfaces (Controller)**: HTTP 요청/응답 매핑. UseCase만 호출. `@Valid`로 입력 형식 검증.
- **Application (UseCase)**: 유스케이스 1개 = 클래스 1개. Repository를 통한 데이터 조회/저장, DB 조회가 필요한 비즈니스 규칙 처리, 여러 도메인 조합, 트랜잭션 경계.
- **Domain**: Entity(JPA 어노테이션 + 비즈니스 로직), VO, Domain Service(순수 함수, Repository 주입 금지), Repository 인터페이스.
- **Infrastructure**: JpaRepository 구현체, 외부 기술 의존.

### 3.1 검증 책임 분리

| 레이어 | 검증 책임 |
|--------|----------|
| Controller | 필수 필드, 형식(이메일, 날짜 등), HTTP 상태 코드 — **요청/응답 계약** |
| Domain | 비밀번호 정책, 이름 형식 등 — **비즈니스 불변식** (자기 데이터만으로 판단) |
| UseCase | DB 조회 필요한 규칙(중복 검증 등), 여러 도메인 조합, 트랜잭션 흐름 제어 |

세부 아키텍처 규칙은 `/.claude/rules/arch.md`를 따릅니다.

### 3.2 도메인 & 객체 설계 전략

도메인 객체는 세 종류이며, **로직이 어디에 있어야 하는가**가 핵심 판단 기준이다.

| 구분 | 상태(필드) | 역할 | 예시 |
|------|:---:|------|------|
| Entity | ✅ 있음 | 자기 데이터를 갖고, 자기 데이터를 바꾸는 비즈니스 행위 | `Product.decreaseStock()` |
| Value Object | ✅ 있음 (불변) | 도메인 규칙을 값 자체에 내장. 변경 시 새 인스턴스 반환 | `Money(amount >= 0)`, `Stock.deduct()` |
| Domain Service | ❌ 없음 | 여러 Entity/VO를 받아 계산/판단만 수행 (순수 함수, **Repository 주입 금지**) | `OrderValidator.validate(items, products)` |

**"이 로직은 어디에?" 판단 기준:**

| 질문 | 답이 "예"면 → |
|------|-------------|
| 자기 데이터만으로 판단 가능한가? | Entity 메서드 |
| 값 자체에 규칙이 있는가? (금액 >= 0, 수량 >= 1) | Value Object |
| 여러 Entity/VO의 데이터가 필요한 순수 검증/계산인가? | Domain Service (Repository 주입 금지) |
| DB 조회가 필요한 비즈니스 규칙인가? | UseCase |
| 여러 도메인을 조합하는 흐름 정책인가? | UseCase |

세부 규칙과 코드 패턴은 `/.claude/rules/domain.md`를 따릅니다.

### 3.3 패키지 구성

```
/interfaces/api/{domain}/     ← Controller, DTO
/application/{domain}/         ← UseCase, Command(입력 DTO), Info(출력 DTO)
/domain/{domain}/              ← Entity, VO, Domain Service, Repository(interface)
/infrastructure/{domain}/      ← JpaRepository
/support/                      ← PageResult, ApiResponse
/support/error/                ← Exception, ErrorCode
```

### 3.4 BaseEntity 상속 전략

BaseEntity는 `id`, `createdAt`, `updatedAt`만 제공한다. `deletedAt`은 삭제의 맥락이 도메인마다 다르므로(판매 중지 vs 탈퇴 vs 취소) 각 엔티티가 직접 선언한다.

| 엔티티 | BaseEntity | deletedAt | 이유 |
|--------|:---:|:---:|------|
| User, Order | ✅ | ❌ | 탈퇴 미구현 / 상태로 관리 |
| Brand, Product | ✅ | ✅ 직접 선언 | soft delete |
| Like, OrderItem | ❌ | ❌ | hard delete / 불변 스냅샷 |

## 4. 개발 원칙 (TDD / 테스트 전략)

### 4.1 TDD 진행 방식

- **기본 사이클**: Red → Green → Refactor
- **OUT TO IN** (탑다운): Controller Test → Controller → Service Test → Service → Repository 순서로 **요청/응답 계약을 먼저 고정**
- **Inside-Out** (도메인 병렬): Password, Email, BirthDate 등 도메인 규칙은 **Domain Unit Test로 먼저 고정**

**요청**: 새 기능마다 아래 순서로 제안해 주세요.
1. Domain Unit Test 케이스 목록 (핵심 불변식)
2. UseCase 통합 테스트 시나리오 (DB 의존 규칙 검증)
3. E2E Test 시나리오 (HTTP 플로우)

### 4.2 테스트 피라미드 & Mock / Assertion 규칙

| 레벨 | 대상 | Mock 사용 |
|------|------|-----------|
| Unit | 도메인 규칙, 값 객체 | ❌ 사용하지 않음 |
| Integration | UseCase + Repository | ⭕ 외부 의존성 격리 시 |
| E2E | 전체 HTTP 흐름 | ❌ 사용하지 않음 |

**단언문 규칙**: 테스트당 **핵심 단언문 1개**. "이 테스트가 정말 검증하고 싶은 것"에만 집중.

자세한 테스트 규칙은 `/.claude/rules/testing.md`를 참고해 주세요.

## 5. AI 협업 규칙 (Claude Code 사용 원칙)

- **설계 주도권은 개발자(사용자)**에게 있고, Claude는 **제안 도구**로 동작합니다.
- 레이어 책임 이동, 도메인 모델 변경, 인증/보안 로직 변경 전에는 **항상 요약 & 설계안**부터 제안해 주세요.

### 5.1 워크플로우 (Explore → Plan → Code → Test)

1. **Explore**: 관련 파일/테스트를 읽고, 현재 설계/레이어/도메인 규칙을 요약. 코드 수정 X.
2. **Plan**: 변경을 1 커밋 단위 TODO로 나누어 제안. TDD 순서와 레이어 책임 분리 고려.
3. **Code**: 승인된 TODO 한 개씩만 구현. 변경된 파일, 레이어별 책임, 테스트 영향 범위 요약.
4. **Test**: 어떤 테스트(Unit/Integration/E2E)를 추가/수정했는지와 커버하는 시나리오 설명.

Driver / Navigator 모드는 `/.claude/rules/ai-workflow.md`에 정의합니다.

## 6. 금지 사항 (Never Do)

- 실패하는 테스트를 없애기 위해 테스트 삭제 또는 단언문 제거
- 비즈니스 로직을 Controller에 몰아 넣기
- Domain Service에 Repository를 주입하는 것
- 도메인 규칙을 편의상 Controller로 옮기기 (필요 시 설계 변경 이유 먼저 설명)
- 가짜 Mock 데이터에만 의존하는, 실제로 동작하지 않는 구현
- null-safety를 깨는 구현

## 7. 우선순위

1. 실제로 동작하고, 테스트 가능한 구조
2. 도메인 규칙이 Domain에 모여 있는 설계
3. null-safety, thread-safety
4. 테스트 커버리지와 가독성
5. 기존 코드 스타일/패턴과의 일관성

## 8. 라운드별 결정 기록

매 라운드(주차)에서 내린 설계 결정은 `/.claude/rounds/` 폴더에 기록합니다.
- `/.claude/rounds/round1.md` - Round 1: 사용자 인증 기능
- `/.claude/rounds/round3.md` - Round 3: 도메인 모델링 (Brand, Product, Like, Order)

Claude는 이전 라운드에서 정한 원칙을 기억한 상태로 다음 기능을 도와주세요.
