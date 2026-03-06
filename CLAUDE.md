# CLAUDE.md

**한국어(Korean)**로 응답한다. 코드는 예외이다.

## 프로젝트 개요

감성 이커머스 플랫폼 — 좋아요, 쿠폰, 주문/결제 기능을 갖춘 Multi-module Spring Boot(Kotlin) 애플리케이션.
서비스 흐름: 회원가입 → 브랜드/상품 탐색 → 좋아요 → 쿠폰 발급 → 주문/결제.

요구사항 및 API 스펙은 `docs/` 디렉토리의 요구사항 문서를 참조한다.
완성된 API는 `.http/<도메인명>/` 디렉토리에 테스트 파일로 정리한다.

### 인증 방식

- **대고객 API** (`/api/v1`): 헤더 `X-Loopers-LoginId`, `X-Loopers-LoginPw`로 유저 식별. 별도 인증/인가 구현 없음.
- **어드민 API** (`/api-admin/v1`): 헤더 `X-Loopers-Ldap: loopers.admin`으로 어드민 식별.

### 향후 고도화 목표

기능 구현 완료 후, 동시성 / 멱등성 / 데이터 일관성 / 느린 조회 최적화 / 트랜잭션 실패 복구를 해결한다.

## AI 행동 가이드라인

속도보다 **신중함**에 무게를 둔다.

**코딩 전에 생각하라:**
- 가정을 명시적으로 밝혀라. 확실하지 않으면 질문하라.
- 여러 해석이 가능하면 모두 제시하라 — 조용히 하나를 고르지 마라.
- **읽기 우선**: 수정 대상 코드를 반드시 먼저 읽어라. 읽지 않고 수정하지 마라.
- **환각 금지**: 존재하지 않는 API, 패키지, 파일 경로, 설정 옵션을 지어내지 마라.

**단순함 우선:**
- 요청받지 않은 기능, 추상화, "유연성"을 넣지 마라.
- "시니어 엔지니어가 오버엔지니어링이라고 할까?" 그렇다면 단순화하라.

**외과적 변경:**
- 주변 코드, 주석, 포매팅을 "개선"하지 마라. 기존 스타일에 맞춰라.
- 관련 없는 데드코드는 언급만 하라 — 삭제하지 마라.
- 본인의 변경으로 불필요해진 import/변수/함수만 제거하라.
- 검증 기준: **변경된 모든 라인은 사용자의 요청에 직접 연결**되어야 한다.

**실행 원칙:**
- 작업을 검증 가능한 목표로 변환하라 (예: "버그 수정" → "재현 테스트 작성 후 통과시켜라").
- 대규모 변경을 한 번에 하지 마라. 작은 단위로 나눠서 각 단계마다 검증하라.
- 에러 발생 시 근본 원인을 분석하라. 같은 시도를 반복하지 마라.
- 방향이 틀렸으면 매몰 비용에 집착하지 말고 과감히 다시 작성하라.

**피드백 반영:**
- 1회 지적 → `MEMORY.md`에 교훈 기록
- 2회 이상 반복 → `~/.claude/rules/corrections.md`로 승격

## 커뮤니케이션 원칙

모든 산출물(코드, 문서, 커밋 메시지, PR 설명)은 **저맥락 커뮤니케이션** 원칙을 따른다.
축약어와 키워드 나열을 지양하고, 동료 개발자가 추가 설명 없이 이해할 수 있도록 의도를 명확히 서술한다.

- 좋은 예: `feat: 비밀번호 변경 시 기존 비밀번호 일치 여부를 검증하는 로직 추가`
- 나쁜 예: `feat: pw chg validation`

## 필수 명령어

```bash
make init                              # pre-commit hook 설치 (ktlint)
docker-compose -f ./docker/infra-compose.yml up -d   # MySQL, Redis, Kafka 기동

./gradlew build                        # 전체 빌드
./gradlew :apps:commerce-api:test      # 특정 모듈 테스트
./gradlew test jacocoTestReport        # 테스트 + 커버리지
./gradlew ktlintCheck                  # 린트 검사
./gradlew ktlintFormat                 # 린트 자동 수정
./gradlew :apps:commerce-api:bootRun   # API 서버 실행
```

커밋 실패 시 `./gradlew ktlintFormat` 후 재시도.

## 아키텍처

### 멀티 모듈 구조

| 계층 | 역할 | 모듈 |
|------|------|------|
| `apps/` | 실행 가능한 Spring Boot 애플리케이션 | `commerce-api`, `commerce-batch`, `commerce-streamer` |
| `modules/` | 도메인 무관 재사용 인프라 설정 | `jpa`, `redis`, `kafka` |
| `supports/` | 횡단 관심사 유틸리티 | `jackson`, `logging`, `monitoring` |

의존성: `apps/` → `modules/`, `supports/`. 역방향 의존 금지.

### 레이어드 아키텍처 + DIP

```
Presentation → Application → Domain ← Infrastructure
```

```
interfaces/       Controllers, DTOs, API specs
application/      Facades, Application Services (유스케이스 조율)
domain/           Models, Domain Services, Repository 인터페이스
infrastructure/   Repository 구현체, 외부 연동
support/          앱 내 유틸리티 (에러 처리 등)
```

**계층별 책임:**
- **Interfaces**: Application 계층의 유스케이스 호출만 담당. 요청 검증, 응답 매핑.
- **Application**: 비즈니스 흐름 조율. 실질적 로직은 도메인으로 위임.
- **Domain**: 비즈니스 로직의 중심. 다른 계층에 의존하지 않음. 모든 의존 방향은 도메인을 향함.
- **Infrastructure**: 도메인 인터페이스의 구현. 외부 기술에 의존.

**계층 간 의존성 규칙:**
- Repository 인터페이스는 도메인 계층에 정의되므로 Application 계층에서 직접 사용 가능. 단, 도메인 서비스로 경계를 가둔 경우에는 도메인 서비스를 통해서만 접근.
- Facade가 단순 흐름 제어만 담당하면 도메인 서비스/Repository를 직접 조합. 복잡한 도메인 객체 간 상호작용이 필요하면 Application Service를 별도로 둔다.

```
[단순]  Controller → Facade → Domain Service / Repository
[복잡]  Controller → Facade → Application Service → Domain Service / Repository
```

**실제 구현 참고** (commerce-api, member 도메인):
- Domain Model: `domain/member/MemberModel.kt`
- Domain Service: `domain/member/MemberService.kt`
- Facade: `application/member/MemberFacade.kt`
- Controller: `interfaces/api/member/MemberV1Controller.kt`

### 아키텍처 & 패키지 구성 전략

- API Request/Response DTO와 Application 계층의 Info DTO는 분리한다.
- 패키지 구성: 4개 레이어 패키지 아래에 도메인별로 하위 패키지를 둔다.
  - `/interfaces/api/{domain}` — Presentation 레이어
  - `/application/{domain}` — Application 레이어 (도메인을 조합해 유스케이스 제공)
  - `/domain/{domain}` — Domain 레이어 (Entity, VO, Domain Service, Repository 인터페이스)
  - `/infrastructure/{domain}` — Infrastructure 레이어 (Repository 구현체)
- Application Layer는 경량으로 유지한다. 실질적 비즈니스 로직은 Domain에 위임한다.

## 도메인 모델링 원칙

도메인 모델링은 현실 세계의 개념과 규칙을 객체 지향적으로 표현하는 작업이다.
핵심은 데이터가 아니라 **행위의 주체와 책임**이다.

| 개념 | 판단 기준 | 예시 |
|------|----------|------|
| **Entity** | 고유 ID로 식별, 상태 변화와 연속성이 중요 | `User`, `Order`, `Product` |
| **Value Object** | 값 자체가 의미, 불변, 동등성은 값으로 판단 | `Money`, `Address`, `Quantity` |
| **Domain Service** | 특정 Entity에 속하기 어려운 도메인 로직, 상태 없음 | `PointChargingService` |

- 비즈니스 의미가 커질 수 있는 개념은 독립된 도메인 단위로 격리한다 (예: 좋아요는 `Product.likedUserIds`가 아닌 독립된 `Like` 도메인).
- Service는 상태를 갖지 않으며, Input과 Output이 명확하다.

### 도메인 & 객체 설계 전략

- 도메인 객체는 비즈니스 규칙을 캡슐화한다. 데이터만 들고 있는 빈껍데기 모델(Anemic Domain Model)은 지양한다.
- 규칙이 여러 서비스에 반복적으로 나타나면, 그 규칙은 도메인 객체에 속할 가능성이 높다.
- 각 기능에 대한 책임과 결합도에 대해 개발자의 의도를 확인하고 개발을 진행한다.

## 개발 규칙

### TDD 워크플로우 (Red → Green → Refactor)

모든 테스트는 **Arrange - Act - Assert** 원칙을 따른다.

1. **RED**: 요구사항에 맞는 테스트 작성 → 실패 확인
2. **GREEN**: 테스트 통과하는 최소 구현. 오버엔지니어링 금지.
3. **REFACTOR**: 코드 정리, 불필요한 import 제거, 성능 최적화. 모든 테스트 통과 필수.

### 요구사항 기반 작업 흐름

요구사항 문서(`docs/*.md`)를 기준으로 구현할 때:

1. Phase/단계를 확인하고 순서대로 진행
2. 각 Phase를 TDD(RED → GREEN → REFACTOR)로 진행
3. Phase 완료 시 보고 (완료 요약, 변경 파일, 테스트 결과, 다음 단계) 후 **사용자 승인 대기**
4. 승인 후에만 다음 Phase 진행

**중요:** 사용자 승인 없이 다음 Phase 진행 금지. 요구사항에 없는 기능 임의 추가 금지. 불명확한 요구사항은 구현 전 질문.

### 금지 사항

- 동작하지 않는 코드, 불필요한 mock 데이터를 사용한 구현
- null-safe하지 않은 코드 (Kotlin null safety 활용)
- `println` 사용
- 사용자 승인 없이 테스트 삭제/수정

### 권장 사항

- 실제 API를 호출하는 E2E 테스트 작성
- 완성된 API는 `.http/<도메인명>/<API명>.http` 파일로 정리
- 기존 코드 패턴을 분석하고 일관성 유지

## 기술 스택

- **언어/프레임워크**: Kotlin 2.0.20, Java 21, Spring Boot 3.4.4
- **데이터**: MySQL 8.0, JPA/Hibernate, QueryDSL (Kapt), Redis 7.0
- **메시징**: Kafka 3.5.1
- **테스트**: JUnit 5, SpringMockK, Mockito-Kotlin, Instancio, Testcontainers
- **코드 품질**: ktlint, JaCoCo
- **인증**: BCrypt, JWT (jjwt 0.12.5, HS256)

## API 응답 형식

모든 API는 `ApiResponse<T>` 형식으로 응답한다.

```json
// 성공
{ "meta": { "result": "SUCCESS", "errorCode": null, "message": null }, "data": { ... } }
// 실패
{ "meta": { "result": "FAIL", "errorCode": "Bad Request", "message": "잘못된 요청입니다." }, "data": null }
```

에러 타입은 `support/error/` 디렉토리의 코드를 참조한다.

## 참고 사항

- 모듈 컨테이너(`apps/`, `modules/`, `supports/`)는 모든 task가 비활성화됨 — 실제 모듈에서만 task 실행
- 테스트: `spring.profiles.active=test`, Timezone `Asia/Seoul`, Testcontainers 사용
- Test fixtures: `modules:jpa`, `modules:redis`에서 제공
- 프로젝트 버전: git commit hash (short SHA) 기반 (`build.gradle.kts`의 `getGitHash()`)
