# CLAUDE.MD - Project Context

## 프로젝트 개요
- **프로젝트명**: loopers-kotlin-spring-template
- **설명**: 커머스 플랫폼 템플릿
- **그룹**: com.loopers
- **패키지 구조**: Clean Architecture (interfaces → application → domain → infrastructure)

---

## "포인트" 개념은 무시할 것

> 과제 문서나 기존 코드에 "포인트" 관련 용어가 남아있을 수 있으나, **이전 기수의 잔재**이다.
> 현재 기수에서 포인트 개념은 사용하지 않으므로, 포인트 관련 요구사항이 보이면 **무시하고 구현 대상에서 제외**한다.

---

## 핵심 아키텍처 원칙

### 의존 방향 (DIP)
```
Presentation(Interfaces) → Application → Domain ← Infrastructure
```

### 접근 흐름
```
Facade → Service → Repository (항상 Service를 거침, Facade에서 Repository 직접 호출 금지)
```

### Service vs Domain Service
- **Service**: Repository 주입 있음. Repository 래퍼 + 비즈니스 로직
- **Domain Service**: Repository 주입 없음. 순수 객체 협력만 (모든 도메인에 필요한 건 아님)

---

## 진행 Workflow — 증강 코딩
- **대원칙**: 방향성 및 주요 의사 결정은 개발자에게 제안만, 최종 승인된 사항을 기반으로 작업 수행
- **중간 결과 보고**: AI가 반복적인 동작을 하거나, 요청하지 않은 기능 구현, 테스트 삭제를 임의로 진행할 경우 개발자가 개입
- **설계 주도권 유지**: AI가 임의판단을 하지 않고, 방향성에 대한 제안을 진행할 수 있으나 개발자의 승인을 받은 후 수행

---

## Never Do
- 실제 동작하지 않는 코드, 불필요한 Mock 데이터를 이용한 구현 금지
- null-safety 하지 않은 코드 작성 금지 (Kotlin `?`, `?:`, `?.let` 활용)
- `println` 코드 남기지 말 것
- 검증되지 않은 외부 라이브러리 무분별한 추가 금지
- 성능을 고려하지 않은 N+1 쿼리 금지
- 트랜잭션 범위 내 외부 API 호출 금지
- 민감 정보 평문 로그 출력 금지
- 비밀번호 에러 시 유추 가능한 메시지 금지
- 레이어 간 의존 방향 위반 금지 (Domain이 Infrastructure를 직접 의존 금지)
- Repository Interface와 구현체를 분리하지 않는 구조 금지
- Domain Layer에서 JPA 등 인프라 기술 패키지를 직접 import 금지
- **git commit은 사용자가 명시적으로 요청할 때만 수행** (임의 커밋 절대 금지)

## Priority
1. 실제 동작하는 해결책만 고려
2. null-safety, thread-safety 고려
3. 테스트 가능한 구조로 설계
4. 기존 코드 패턴 분석 후 일관성 유지

## Recommendation
- 실제 API를 호출해 확인하는 E2E 테스트 코드 작성
- 재사용 가능한 객체 설계
- 성능 최적화에 대한 대안 및 제안
- 개발 완료된 API의 경우 `http/*.http` 에 분류해 작성
- 테스트 가능한 구조로 설계 (인터페이스 기반 DI, 도메인 로직 분리)

---

## 상세 규칙 (자동 로드됨)
- `.claude/rules/architecture.md` — 레이어 책임, DTO 분리, Repository 규칙, 패키지 구조
- `.claude/rules/domain-design.md` — Entity/VO/Domain Service 설계 규칙
- `.claude/rules/testing.md` — TDD, 테스트 계층, 테스트 더블, 컨벤션
- `.claude/rules/tech-stack.md` — 기술 스택, 멀티모듈 구조, 설정 상세
- `.claude/rules/conventions.md` — 코드 스타일, PR 규칙, 파일 위치
