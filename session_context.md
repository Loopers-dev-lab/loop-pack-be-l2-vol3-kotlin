# Session Context

> 세션 시작 시 `CLAUDE.md`와 함께 읽어 현재 프로젝트 상태를 파악하는 문서입니다.
> 마지막 업데이트: 2026-03-16

---

## 현재 상태 요약

### 완료된 작업

**기능 (FEAT)**
- FEAT-1~3: 회원 관리 (가입, 내 정보 조회, 비밀번호 수정) — DONE
  - VO 패턴 적용 완료 (`LoginId`, `MemberName`, `Email`, `RawPassword`)
  - Interceptor + ArgumentResolver 인증 중앙화 완료
  - 인증 캐시: Caffeine → Redis 전환 완료 (D41)
- FEAT-4: 브랜드 관리 (어드민 CRUD + 고객 조회) — DONE
  - 어드민: `POST/GET/PUT/DELETE /api-admin/v1/brands`
  - 고객: `GET /api/v1/brands/{brandId}`
  - Soft Delete 캐스케이드 (브랜드 삭제 → 하위 상품 삭제)
- FEAT-5: 상품 관리 (어드민 CRUD + 고객 조회) — DONE
  - 어드민: `POST/GET/PUT/DELETE /api-admin/v1/products` (offset 페이징)
  - 고객: `GET /api/v1/products` (커서 기반 페이징), `GET /api/v1/products/{productId}`
  - 비관적 락(SELECT FOR UPDATE) 재고 차감
  - VO 패턴: ProductName, ProductDescription, StockQuantity
- FEAT-6: 상품 좋아요 — DONE
  - `POST/DELETE /api/v1/products/{productId}/likes`, `GET /api/v1/likes`
  - 양방향 멱등 (BR-L2), 물리 삭제, UNIQUE Constraint
  - Facade에서 exists 사전 조회 → 멱등 200 반환 (D33, D36)
- FEAT-7: 주문 — DONE
  - 고객: `POST /api/v1/orders`, `GET /api/v1/orders`, `GET /api/v1/orders/{orderId}`
  - 어드민: `GET /api-admin/v1/orders`, `GET /api-admin/v1/orders/{orderId}`
  - 스냅샷 저장 (productName, productPrice, brandName)
  - UUID 주문번호, validateOwner 도메인 검증
  - 재고 차감: 비관적 락 (productId 오름차순 정렬 후 SELECT FOR UPDATE)
  - 쿠폰 사용: 낙관적 락 (@Version on IssuedCoupon)
- FEAT-8: 쿠폰 — DONE
  - 어드민: `POST/GET/PUT/DELETE /api-admin/v1/coupons` (템플릿 CRUD)
  - 고객: `POST /api/v1/coupons/{couponId}/issue` (발급), `GET /api/v1/coupons/me` (내 쿠폰)
  - 만료 정책: FIXED_DATE / DAYS_FROM_ISSUE 이중 지원 (D32)
  - 삭제된 템플릿 쿠폰: 이미 발급된 쿠폰은 만료일까지 사용 가능
  - 동시 사용 방지: 낙관적 락 → 409 CONFLICT (D31)

**성능 (Phase 1 전체 완료)**
- REQ-1.1: Virtual Threads 활성화
- REQ-1.2: Tomcat 튜닝 (max-threads=50, max-connections=10000)
- REQ-1.3: DB 커넥션 풀 확장 (max=50)
- REQ-1.4: 회원가입 중복체크 → Unique Constraint 전환
- REQ-1.5: 인증 헤더 통일 및 중앙화

**성능 (Phase 2: 6/9 완료)**
- REQ-2.1: 인증 캐싱 Caffeine → Redis 전환 — DONE (D41)
- REQ-2.4: 상품 Redis 캐시 (상세 5분 + CUD evict, 목록 1분 + LFU) — DONE (D37)
- REQ-2.6: 상품 복합 인덱스 (브랜드+좋아요순, 브랜드+가격순) — DONE (D38)
- REQ-2.7: like_count 배치 집계 (commerce-batch, UPDATE JOIN) — DONE (D39)
- REQ-2.8: EXPLAIN 성능 검증 (브랜드+가격순 17.6x 개선) — DONE (D40)
- REQ-2.9: 브랜드 Redis 캐시 (TTL 10분 + CUD evict) — DONE (D42)
- REQ-2.2~2.3, 2.5: 회원 캐싱, R/W 분리, Lettuce 풀링 — 보류 (부하 테스트 후 판단)

**동시성 제어**
- 재고 차감: 비관적 락 (SELECT FOR UPDATE, productId 오름차순 데드락 방지)
- 쿠폰 사용: 낙관적 락 (@Version, 409 CONFLICT)
- 좋아요: 멱등 처리 (exists 사전 조회 + DataIntegrityViolation catch)

### 다음 구현 대상

현재 FEAT-1~8 + Phase 1~2 성능 개선 완료. 다음 대상:
- FEAT-9: 결제 (요구사항 미정)
- FEAT-10: 랭킹/추천 (요구사항 미정)

### 미착수 (요구사항 미정)
- FEAT-9: 결제, FEAT-10: 랭킹/추천
- Phase 2 보류: R/W 분리, Lettuce 풀링, 회원 캐싱
- Phase 3: Circuit Breaker, Rate Limiting, Graceful Degradation

---

## 핵심 아키텍처 결정 (42건, 상세는 DECISIONS.md)

새 세션에서 반드시 인지해야 할 결정:

| # | 결정 | 요약 |
|---|------|------|
| D1 | Virtual Threads | 설정 1줄로 적용. synchronized 금지 → ReentrantLock |
| D2 | 중복체크 | 사전 조회 금지. Unique Constraint + Facade에서 예외 catch |
| D3 | 인증 패턴 | Interceptor + ArgumentResolver. JWT 미사용 |
| D4 | 캐시 백엔드 | Redis (인증/상품/브랜드). CacheStore 포트 패턴 + RedisTemplate 직접 사용 |
| D5 | 캐시 키 | loginId가 키, SHA256 다이제스트로 비밀번호 비교 |
| D8 | Facade 분리 | Service 공유, 고객/어드민 Facade만 분리. 전 도메인 일관 적용 |
| D9 | 재고 검증 | `ProductModel.deductStock()` 도메인 불변식 + 비관적 락 |
| D10 | 본인 검증 | `OrderModel.validateOwner()` 도메인 규칙 |
| D11 | Soft Delete | status=DELETED + deleted_at 병행, `delete()` 메서드에서 동시 설정 |
| D12 | 좋아요 집계 | product.like_count + 배치 집계 (UPDATE JOIN) |
| D13 | 주문 번호 | UUID |
| D15 | DTO 분리 | Controller 레벨. Facade는 동일 Info 반환 |
| D17 | 좋아요 멱등 | 등록/취소 양방향 멱등. 항상 200 OK |
| D18 | Service 분리 | 고객용(ACTIVE only) / 어드민용(상태 무관) |
| D23 | VO 패턴 | @JvmInline value class + Entity primitive 저장 |
| D25 | 스냅샷 범위 | productName, productPrice, brandName만 복사 |
| D31 | 쿠폰 동시사용 방지 | 낙관적 락 (@Version on IssuedCoupon) → 409 CONFLICT |
| D32 | 쿠폰 만료 정책 | FIXED_DATE / DAYS_FROM_ISSUE 이중 지원 |
| D33 | 좋아요 멱등 전략 | Facade exists 사전 조회 허용 (멱등 200 목적) |
| D37 | 캐시 구현 방식 | RedisTemplate 직접 사용 + CacheStore 포트 (DIP) |
| D38 | 상품 인덱스 | 복합 인덱스 (brand_id, status, like_count/price, id) |
| D39 | 배치 집계 | Tasklet + UPDATE JOIN 단일 쿼리 (Chunk 미사용) |
| D41 | 인증 캐시 전환 | Caffeine → Redis (멀티 인스턴스 BCrypt 중복 제거) |
| D42 | 브랜드 캐시 | TTL 10분 + CUD evict. 상품 조회 시 N+1 방지 |

---

## 새 도메인 추가 시 확장 패턴

example 패키지를 참고하여 4-layer 구조 유지:

```
interfaces/api/{domain}/        → Controller, ApiSpec, Dto (값 유효성 검증)
interfaces/api/admin/{domain}/  → AdminController, AdminApiSpec, AdminDto
application/{domain}/           → Facade (cross-domain만), Service (단일 도메인 로직), Info
domain/{domain}/                → Model(Entity), Repository(interface), VO, Validator, Command
infrastructure/{domain}/        → RepositoryImpl, JpaRepository
```

- 단일 도메인 API: Controller → Service 직접 호출 (Facade 경유 X)
- cross-domain API: Controller → Facade → Service
- Service는 공유, Facade만 고객/어드민 분리
- Application Layer가 트랜잭션의 주체 (Facade + Service)
- VO: 단일 필드 값 검증 (`domain/{domain}/vo/` 또는 `domain/common/vo/`)
- Validator: VO로 검증 불가능한 복합 비즈니스 규칙 (`domain/{domain}/`)
- 물리 FK 미사용, 논리적 참조만

---

## 금지사항 (빠른 참조)

- `synchronized` 사용 금지 → `ReentrantLock`
- `saveAndFlush()` 사용 금지
- 사전 조회로 중복체크 금지 → Unique Constraint
- 매 요청 BCrypt 반복 금지 → 캐싱
- println 코드 금지
- null-safety 위반 금지

---

## 개발 워크플로우

**TDD (Red → Green → Refactor)**
- 3A 원칙: Arrange - Act - Assert
- 테스트 프레임워크: JUnit 5 + MockK + TestContainers

**증강 코딩 원칙**
- 방향성/의사결정은 개발자 승인 후 수행
- 모호한 요구사항은 진행 전 확인 질문
- 새 요구사항 확정 → `REQUIREMENTS.md` 추가
- 기술 판단 발생 → `DECISIONS.md` 기록

---

## 참조 문서

| 문서 | 위치 | 용도 |
|------|------|------|
| CLAUDE.md | 프로젝트 루트 | 전체 프로젝트 규칙, 모듈 구조, 기술 스택, 로드맵 |
| REQUIREMENTS.md | 프로젝트 루트 | 기능/성능 요구사항 상세 (REQ 번호, 수용 기준, 상태) |
| DECISIONS.md | 프로젝트 루트 | 기술 판단 기록 (배경, 선택지, 근거, 트레이드오프) |
| docs/design/ | 01-requirements, 02-sequence, 03-class, 04-erd | 설계 다이어그램 |
