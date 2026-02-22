# Commerce API 아키텍처

## 레이어 구조 & 의존 방향

```
interfaces/  →  application/  →  domain/  ←  infrastructure/
```

**DIP**: `Application → Domain ← Infrastructure`. Domain은 어디에도 의존하지 않는다.

---

## Domain — 비즈니스 규칙의 핵심

### Entity: 도메인 비즈니스 (자기 상태를 스스로 변경/판단)

| Entity      | 주요 메서드                                                                                  | 책임                                                        |
|-------------|-----------------------------------------------------------------------------------------|-----------------------------------------------------------|
| `Product`   | `decreaseStock()`, `increaseStock()`, `update()`, `isActive()`, `isAvailableForOrder()` | 재고 증감 시 상태 자동 전환(ON_SALE↔SOLD_OUT), 상품 정보 수정, 주문 가능 여부 판단 |
| `Brand`     | `update()`, `isDeleted()`                                                               | 브랜드명 수정, 삭제 상태 판단                                         |
| `User`      | `changePassword()`, `verifyPassword()`, `getMaskedName()`                               | 현재 비밀번호 검증 후 변경, 이름 마스킹                                   |
| `UserPoint` | `charge()`, `use()`, `canAfford()`                                                      | 잔액 충전/차감, 잔액 충분한지 판단. MAX_BALANCE 초과 방지                   |
| `Order`     | `create()` (companion)                                                                  | private constructor + 팩토리. 생성 후 불변                        |
| `OrderItem` | `create()` (companion)                                                                  | `OrderProductInfo`를 받아 생성. Product 직접 의존 차단               |

### Domain Service: 애플리케이션 비즈니스 (엔티티 협력 조율 + 교차 검증)

| Service                | 주요 검증/조율                                                                                  | Entity 위임                                                       |
|------------------------|-------------------------------------------------------------------------------------------|-----------------------------------------------------------------|
| `CatalogService`       | 상품 생성 시 **브랜드 존재 + 미삭제 확인**, 브랜드 삭제 시 **소속 상품 일괄 삭제**, 주문용 상품 조회 시 **전체 존재 + ON_SALE 검증** | `product.decreaseStock()`, `brand.update()` 등 상태 변경은 Entity에 위임 |
| `UserService`          | 회원가입 시 **loginId 중복 검증**                                                                  | `user.changePassword()`에 위임                                     |
| `PointChargingService` | **충전 금액 범위 검증** (0 < amount ≤ 10M), 비관적 락 획득                                              | `userPoint.charge()`에 위임 + PointHistory 기록                      |
| `UserPointService`     | 포인트 사용 시 비관적 락 획득                                                                         | `userPoint.use()`에 위임 + PointHistory 기록                         |
| `OrderService`         | 주문 조회 시 **소유자 확인** (userId 일치 검증)                                                         | `Order.create()`, `OrderItem.create()`로 생성 위임                   |
| `LikeService`          | **좋아요 중복 방지** (이미 존재하면 false 반환)                                                          | —                                                               |

### 그 외 Domain 구성 요소

| 구성 요소              | 책임                  | 예시                                                         |
|--------------------|---------------------|------------------------------------------------------------|
| Repository (인터페이스) | 도메인 언어로 표현된 저장소 계약  | `findByUserIdForUpdate` (락 의도를 이름으로 표현)                    |
| Value Object       | 생성 시점에 자가 검증하는 불변 값 | `Stock`, `Point`, `LoginId`, `Password`, `Name`            |
| Command            | 서비스 요청 파라미터 묶음      | `CatalogCommand.CreateProduct`, `OrderCommand.CreateOrder` |

---

## Application — Cross-Domain 오케스트레이션

여러 Domain Service를 조합하는 **Facade**. 비즈니스 로직 자체는 Domain에 위임하고, 흐름만 조율.

| Facade        | 조합하는 서비스                                         | 하는 일                                   |
|---------------|--------------------------------------------------|----------------------------------------|
| `UserFacade`  | UserService + UserPointService                   | 회원가입: User 생성 → UserPoint 초기화          |
| `OrderFacade` | CatalogService + OrderService + UserPointService | 주문 생성: 상품 검증 → 재고 차감 → 주문 생성 → 포인트 차감  |
| `LikeFacade`  | LikeService + CatalogService                     | 좋아요: Like 등록/취소 + Product.likeCount 연동 |

---

## Infrastructure — Repository 구현

Domain Repository 인터페이스의 JPA 구현체. **RepositoryImpl → JpaRepository** 2단 위임 구조.

---

## Interfaces — API 진입점

Controller → Dto 변환 → Service/Facade 호출. OpenAPI 명세는 ApiSpec 인터페이스로 분리.

**특이사항:**

- **계층 뛰어넘기 허용**: 단일 도메인이면 Controller → Domain Service 직접 호출 (Facade 생략)
- **인증**: `AuthInterceptor`가 UserService를 직접 호출하여 헤더 기반 인증 처리
- **도메인 타입 변환**: `PageResult<T>.toSpringPage()` 확장 함수로 Controller에서 Spring 타입으로 변환

---

## 주요 설계 결정

1. **DIP**: Domain에 Repository 인터페이스, Infrastructure에서 구현
2. **Entity 생성 이원화**: 상태 변경 엔티티(`constructor + guard()`) vs 불변 엔티티(`private constructor + create()`)
3. **VO 자가 검증**: 생성 시점에 도메인 규칙 강제 (`Stock`, `Point`, `LoginId` 등). 단, 한 줄짜리 단순 검증(OrderItem의 quantity, Brand의 name 등)은
   별도 VO 없이 `guard()` 메서드에서 직접 검증하는 방향으로 정리함. User의 VO(`LoginId`, `Name`, `Password`)는 `guard()`로 합치면 메서드가 과도하게 길어져 아직
   VO 유지 중
4. **Anti-Corruption Layer**: `OrderProductInfo`로 도메인 간 직접 의존 차단. Order 도메인이 Product 엔티티를 직접 참조하지 않고, 필요한 정보만 자기 도메인의
   데이터 클래스로 수신
5. **비관적 락**: 포인트 동시성 제어를 도메인 언어(`findByUserIdForUpdate`)로 표현
6. **도메인 고유 타입**: `PageResult<T>`로 Spring 타입 오염 방지
