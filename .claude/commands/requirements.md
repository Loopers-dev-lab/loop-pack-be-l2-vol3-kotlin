---
name: requirements
description: "새로운 기능의 요구사항 문서를 프로젝트 패턴에 맞춰 자동 생성합니다"
category: design
complexity: intermediate
---

# /requirements - 기능 요구사항 문서 생성

## Triggers
- 새로운 기능 구현 전 요구사항 문서가 필요한 경우
- `/requirements {기능명}` 형태로 직접 호출

## Usage
```
/requirements {기능명(한글)}
```

**출력 경로**: `docs/{기능명}/01-requirements.md`

## Behavioral Flow

### 1단계: 프로젝트 패턴 및 도메인 컨텍스트 분석
다음 파일들을 읽어 프로젝트의 기존 패턴과 Round2 도메인 전체 맥락을 파악합니다:
- `CLAUDE.md`: 프로젝트 구조, 레이어 아키텍처, API 응답 형식, 에러 타입
- `docs/공통/서비스-흐름-예시.md`: 전체 서비스 흐름 (회원가입 → 상품 탐색 → 좋아요 → 주문)
- `docs/공통/API-제안-사항.md`: API prefix 규칙, 인증 방식 (헤더 기반)
- 해당 기능의 API 스펙 참고 문서 (아래 매핑 참조)

**기능명 → API 스펙 참고 문서 매핑:**
| 기능명 | 참고 문서 경로 |
|--------|--------------|
| 유저 | `docs/유저/유저-API-스펙.md` |
| 브랜드-상품 | `docs/브랜드-상품/브랜드-상품-API-스펙.md` |
| 브랜드-상품-Admin | `docs/브랜드-상품/브랜드-상품-Admin-API-스펙.md` |
| 좋아요 | `docs/좋아요/좋아요-API-스펙.md` |
| 주문 | `docs/주문/주문-API-스펙.md` |
| 주문-Admin | `docs/주문-Admin/주문-Admin-API-스펙.md` |
- `apps/commerce-api/src/main/kotlin/com/loopers/support/error/ErrorType.kt`: 에러 타입 열거형
- `apps/commerce-api/src/main/kotlin/com/loopers/interfaces/api/ApiResponse.kt`: API 응답 형식

**Round2 도메인 전체 맥락:**
- 유저(Users), 브랜드(Brands), 상품(Products), 좋아요(Likes), 주문(Orders) 도메인이 존재합니다.
- 각 도메인은 서로 연관되어 있으므로, 요구사항 작성 시 관련 도메인과의 의존 관계를 명시해야 합니다.
- 대고객 API는 `/api/v1` prefix, 어드민 API는 `/api-admin/v1` prefix를 사용합니다.
- 대고객 인증: `X-Loopers-LoginId`, `X-Loopers-LoginPw` 헤더 / 어드민 인증: `X-Loopers-Ldap` 헤더

### 2단계: 사용자와 요구사항 협의
AskUserQuestion 도구를 활용하여 다음 사항을 확인합니다:
- 기능의 핵심 유스케이스 (유저가 어떤 행동을 하고, 어떤 결과를 기대하는가?)
- API 엔드포인트 구성 (HTTP 메서드, 경로, Request/Response 구조)
- 인증 필요 여부 (대고객: `X-Loopers-LoginId/LoginPw` 헤더 / 어드민: `X-Loopers-Ldap` 헤더)
- 관련 도메인과의 의존 관계 (유저, 브랜드, 상품, 좋아요, 주문 중 어떤 도메인과 연관되는지)
- 새로운 도메인 모델이 필요한지 여부

### 3단계: 문서 생성
기존 요구사항 문서와 동일한 Part 기반 구조로 문서를 생성합니다.

**유저 중심 서술 원칙:**
- 모든 기능 요구사항은 "유저가 ~한다", "어드민이 ~한다" 형태의 유저 행동 중심으로 서술합니다.
- 기술적 구현 세부사항보다 유저의 의도와 기대 결과를 먼저 명시합니다.
- 예: "유저가 상품에 좋아요를 누르면, 좋아요 수가 증가하고 좋아요 목록에 추가된다"

**도메인 간 관계 명시 원칙:**
- 해당 기능이 의존하는 다른 도메인을 명확히 기술합니다.
- 예: 주문 기능은 유저(주문자), 상품(주문 대상), 브랜드(상품 소속)와 연관됩니다.

**문서 구조:**

```markdown
# {기능명} 요구사항

## 개요
{기능에 대한 1~2문장 설명}

## 관련 도메인
{이 기능이 의존하거나 연관된 도메인 목록과 관계 설명}

---

## Part 1: API 명세
### 1.1 Endpoint
### 1.2 Request Body
### 1.3 Response (성공)
### 1.4 Response (실패)

---

## Part 2: 비즈니스 규칙
### 2.1 필수 규칙
### 2.2 추가 규칙 (해당되는 경우)

---

## Part 3: 구현 컴포넌트
### 3.1 레이어별 구조
### 3.2 처리 흐름

---

## Part 4: 구현 체크리스트
### Phase 1: ...
### Phase 2: ...
(각 Phase에 TDD용 체크리스트 포함)

---

## Part 5: 테스트 시나리오
### 5.1 단위 테스트
### 5.2 통합 테스트
### 5.3 E2E 테스트

---

## Part 6: 보안 고려사항

---

## Part 7: 검증 명령어
```

### 4단계: 품질 체크리스트 자가 검증
문서 완성 후 다음 체크리스트를 스스로 검증하고, 검증 결과를 문서 하단에 포함합니다:

```markdown
## 품질 체크리스트
- [ ] 상품/브랜드/좋아요/주문 등 관련 도메인과의 관계가 명시되어 있는가?
- [ ] 기능 요구사항이 유저 중심("유저가 ~한다")으로 서술되어 있는가?
- [ ] 인증 방식(헤더 기반)이 정확히 명시되어 있는가?
- [ ] 에러 케이스와 예외 상황이 포함되어 있는가?
- [ ] Phase별 구현 체크리스트가 TDD 워크플로우에 맞게 구성되어 있는가?
```

### 5단계: 검토 요청
생성된 문서를 사용자에게 보여주고 피드백을 요청합니다.

## Tool Coordination
- **Read**: 기존 요구사항 문서, CLAUDE.md, ErrorType.kt, ApiResponse.kt 분석
- **Grep**: 기존 도메인 모델 및 컨트롤러에서 관련 패턴 검색
- **Glob**: 프로젝트 구조 탐색 (레이어별 파일 배치 확인)
- **Write**: 요구사항 문서 파일 생성
- **AskUserQuestion**: 요구사항 세부사항 협의

## Key Patterns

### API 응답 형식
모든 API는 `ApiResponse<T>` 형식을 따릅니다:
```json
{
  "meta": {
    "result": "SUCCESS" | "FAIL",
    "errorCode": null | "에러 코드",
    "message": null | "에러 메시지"
  },
  "data": { ... } | null
}
```

### 에러 타입
`ErrorType` 열거형에 정의된 에러 코드를 사용합니다:
- `INTERNAL_ERROR` (500), `BAD_REQUEST` (400), `NOT_FOUND` (404)
- `CONFLICT` (409), `UNAUTHORIZED` (401), `TOKEN_EXPIRED` (401), `INVALID_TOKEN` (401)

### 레이어 구조
```
interfaces/api/{도메인}/   - Controller, ApiSpec, DTO
application/{도메인}/      - Facade, Info DTO
domain/{도메인}/           - Model, Service, Repository(인터페이스)
infrastructure/{도메인}/   - RepositoryImpl, JpaRepository
support/                   - 유틸리티, 에러 핸들링
```

### Phase 체크리스트 작성 규칙
- 각 Phase는 TDD 워크플로우(Red → Green → Refactor)에 맞게 구성
- Phase 단위로 사용자 검토 및 승인이 가능하도록 적절한 크기로 분할
- 테스트 항목을 각 Phase에 포함

## Examples

### 요구사항 문서 생성
```
/requirements 상품 관리
# → docs/상품-관리/01-requirements.md 생성
# 사용자와 유스케이스를 협의한 뒤 Part 1~7 구조의 요구사항 문서를 작성합니다.
```

### 단일 API 기능
```
/requirements 주문 생성
# → docs/주문-생성/01-requirements.md 생성
# 주문 생성 API의 엔드포인트, 비즈니스 규칙, 구현 체크리스트를 포함합니다.
```

## Boundaries

**수행하는 작업:**
- 프로젝트 패턴을 분석하여 일관된 구조의 요구사항 문서 생성
- 사용자와 대화를 통해 요구사항을 구체화
- TDD 워크플로우에 맞는 Phase별 구현 체크리스트 제공

**수행하지 않는 작업:**
- 사용자 확인 없이 임의로 요구사항을 결정
- 코드 구현 (요구사항 문서만 생성)
- 기존 문서를 사용자 확인 없이 덮어쓰기
