---
name: class-diagram
description: "요구사항 문서를 기반으로 레이어별 Mermaid 클래스 다이어그램을 자동 생성합니다"
category: design
complexity: intermediate
---

# /class-diagram - 클래스 다이어그램 문서 생성

## Triggers
- 요구사항 문서 작성 후 클래스 설계가 필요한 경우
- `/class-diagram {기능명}` 형태로 직접 호출

## Usage
```
/class-diagram {기능명(한글)}
```

**출력 경로**: `docs/{기능명}/03-class-diagram.md`
**전제조건**: `docs/{기능명}/01-requirements.md`가 먼저 존재해야 합니다.

## Behavioral Flow

### 1단계: 요구사항 문서 및 도메인 컨텍스트 확인
- `docs/{기능명}/01-requirements.md` 파일이 존재하는지 확인합니다.
- 파일이 없으면 `/requirements {기능명}` 스킬을 먼저 실행하도록 안내하고 중단합니다.
- 요구사항 문서에서 구현 컴포넌트(Part 3)와 처리 흐름을 파악합니다.
- `docs/공통/API-제안-사항.md`를 읽어 인증 방식과 API prefix 규칙을 확인합니다.
- 관련 도메인의 API 스펙 참고 문서를 확인하여 도메인 간 의존 관계를 파악합니다.

**기능명 → API 스펙 참고 문서 매핑:**
| 기능명 | 참고 문서 경로 |
|--------|--------------|
| 유저 | `docs/유저/유저-API-스펙.md` |
| 브랜드-상품 | `docs/브랜드-상품/브랜드-상품-API-스펙.md` |
| 브랜드-상품-Admin | `docs/브랜드-상품/브랜드-상품-Admin-API-스펙.md` |
| 좋아요 | `docs/좋아요/좋아요-API-스펙.md` |
| 주문 | `docs/주문/주문-API-스펙.md` |
| 주문-Admin | `docs/주문-Admin/주문-Admin-API-스펙.md` |

**도메인 설계 표현 원칙:**
- 클래스 구조가 비즈니스 도메인의 개념과 관계를 정확히 반영해야 합니다.
- 도메인 모델(Model)에는 비즈니스 로직과 검증 메서드를 포함하여, 빈약한 도메인 모델(Anemic Domain Model)을 지양합니다.
- 여러 도메인이 관련된 기능(예: 주문은 유저+상품+브랜드)에서는 도메인 간 참조 관계를 명확히 표현합니다.
- Facade가 여러 도메인 Service를 조합하는 경우, 각 Service 의존성을 모두 표현합니다.

### 2단계: 기존 클래스 패턴 분석
실제 소스 코드를 읽어 프로젝트의 클래스 설계 패턴을 파악합니다:

**Domain 레이어:**
- `domain/member/MemberModel.kt`: Entity 클래스 패턴 (BaseEntity 상속, 필드 정의, 검증 메서드)
- `domain/member/MemberService.kt`: Service 클래스 패턴
- `domain/member/MemberRepository.kt`: Repository 인터페이스 패턴

**Application 레이어:**
- `application/member/MemberFacade.kt`: Facade 클래스 패턴 (Service 조합, DTO 변환)
- `application/member/MemberInfo.kt`: Info DTO 패턴

**Interfaces 레이어:**
- `interfaces/api/member/MemberV1Controller.kt`: Controller 패턴 (ApiSpec 구현)
- `interfaces/api/member/MemberV1ApiSpec.kt`: API 스펙 인터페이스 패턴
- `interfaces/api/member/MemberV1Dto.kt`: Request/Response DTO 패턴

**Infrastructure 레이어:**
- `infrastructure/member/MemberRepositoryImpl.kt`: Repository 구현체 패턴
- `infrastructure/member/MemberJpaRepository.kt`: JpaRepository 인터페이스 패턴

### 3단계: Mermaid 클래스 다이어그램 작성
레이어별로 구분된 클래스 다이어그램을 작성합니다:

**1) 전체 레이어 관계 다이어그램:**
- 각 레이어의 주요 클래스와 의존 관계를 한눈에 보여주는 다이어그램
- 레이어간 의존 방향 표현 (interfaces → application → domain ← infrastructure)

**2) Domain 레이어 상세:**
```mermaid
classDiagram
    class BaseEntity {
        <<abstract>>
        +Long id
        +ZonedDateTime createdAt
        +ZonedDateTime updatedAt
        +ZonedDateTime? deletedAt
        +guard() void
        +delete() void
        +restore() void
    }

    class {도메인}Model {
        -String 필드1
        -Long 필드2
        +비즈니스메서드() void
        -validate필드() void
    }

    BaseEntity <|-- {도메인}Model

    class {도메인}Repository {
        <<interface>>
        +findById(id: Long) {도메인}Model
        +save({도메인}Model) {도메인}Model
    }

    class {도메인}Service {
        -{도메인}Repository repository
        +findById(id: Long) {도메인}Model
        +create(...) {도메인}Model
    }

    {도메인}Service --> {도메인}Repository
```

**3) Application 레이어 상세:**
- Facade 클래스와 Info DTO의 관계
- Service 의존성 주입 표현

**4) Interfaces 레이어 상세:**
- Controller, ApiSpec 인터페이스, Request/Response DTO
- Controller가 ApiSpec을 구현하는 관계

**5) Infrastructure 레이어 상세:**
- RepositoryImpl이 domain의 Repository 인터페이스를 구현
- JpaRepository 확장 관계

### 4단계: 품질 체크리스트 자가 검증
문서 완성 후 다음 체크리스트를 스스로 검증하고, 검증 결과를 문서 하단에 포함합니다:

```markdown
## 품질 체크리스트
- [ ] 도메인 모델(Model)에 비즈니스 로직과 검증 메서드가 포함되어 있는가? (빈약한 도메인 모델 지양)
- [ ] 여러 도메인이 관련된 경우, 도메인 간 참조 관계가 명확히 표현되어 있는가?
- [ ] Facade가 조합하는 여러 Service 의존성이 모두 표현되어 있는가?
- [ ] 각 레이어(Domain, Application, Interfaces, Infrastructure)의 클래스가 모두 포함되어 있는가?
- [ ] 클래스 간 관계(상속, 구현, 의존, 컴포지션)가 정확히 표현되어 있는가?
```

### 5단계: 검토 요청
생성된 다이어그램 문서를 사용자에게 보여주고 피드백을 요청합니다.

## Tool Coordination
- **Read**: 요구사항 문서(`01-requirements.md`) 및 기존 소스 코드 분석
- **Grep**: 클래스 상속 관계, 인터페이스 구현 패턴 검색
- **Glob**: 레이어별 파일 구조 탐색
- **Write**: 클래스 다이어그램 문서 파일 생성

## Key Patterns

### 프로젝트 네이밍 규칙
| 레이어 | 클래스 유형 | 네이밍 패턴 | 예시 |
|--------|-----------|------------|------|
| domain | Entity | `{도메인}Model` | `ProductModel` |
| domain | Service | `{도메인}Service` | `ProductService` |
| domain | Repository (인터페이스) | `{도메인}Repository` | `ProductRepository` |
| application | Facade | `{도메인}Facade` | `ProductFacade` |
| application | Info DTO | `{도메인}Info` | `ProductInfo` |
| interfaces | Controller | `{도메인}V1Controller` | `ProductV1Controller` |
| interfaces | API Spec | `{도메인}V1ApiSpec` | `ProductV1ApiSpec` |
| interfaces | DTO | `{도메인}V1Dto` | `ProductV1Dto` |
| infrastructure | Repository 구현체 | `{도메인}RepositoryImpl` | `ProductRepositoryImpl` |
| infrastructure | JPA Repository | `{도메인}JpaRepository` | `ProductJpaRepository` |

### 클래스 다이어그램 관계 표현
- `<|--`: 상속 (BaseEntity ← Model)
- `<|..`: 인터페이스 구현 (Repository ← RepositoryImpl)
- `-->`: 의존 (Controller → Facade → Service → Repository)
- `*--`: 컴포지션 (Dto 내부 클래스)

### 문서 구조
```markdown
# {기능명} 클래스 다이어그램

## 개요
{이 문서가 다루는 클래스 구조 설명}

---

## 1. 전체 레이어 관계
{Mermaid 클래스 다이어그램}

---

## 2. Domain 레이어 상세
{Mermaid 클래스 다이어그램}

### 클래스 설명
| 클래스 | 역할 | 주요 메서드 |
|--------|------|-----------|

---

## 3. Application 레이어 상세
{Mermaid 클래스 다이어그램}

---

## 4. Interfaces 레이어 상세
{Mermaid 클래스 다이어그램}

---

## 5. Infrastructure 레이어 상세
{Mermaid 클래스 다이어그램}
```

## Examples

### 클래스 다이어그램 생성
```
/class-diagram 상품 관리
# → docs/상품-관리/03-class-diagram.md 생성
# 요구사항 문서를 기반으로 레이어별 클래스 다이어그램을 작성합니다.
```

### 기존 도메인 연관 기능
```
/class-diagram 주문 생성
# → docs/주문-생성/03-class-diagram.md 생성
# 기존 MemberModel, ProductModel 등과의 관계를 포함하여 클래스 다이어그램을 작성합니다.
```

## Boundaries

**수행하는 작업:**
- 요구사항 문서를 기반으로 레이어별 Mermaid 클래스 다이어그램 생성
- 기존 코드베이스의 네이밍 규칙과 패턴을 반영
- BaseEntity 상속, Repository 인터페이스/구현체 분리 등 프로젝트 아키텍처 반영

**수행하지 않는 작업:**
- 요구사항 문서 없이 클래스 다이어그램 생성 (전제조건 미충족 시 안내 후 중단)
- 코드 구현 (클래스 다이어그램 문서만 생성)
- 기존 문서를 사용자 확인 없이 덮어쓰기
