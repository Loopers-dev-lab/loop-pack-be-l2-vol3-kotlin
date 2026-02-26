# Architecture Rules

## 레이어 간 의존성

```
Interfaces → Application (UseCase) → Domain ← Infrastructure
```

- Domain은 아무것도 의존하지 않는다. 모든 의존 방향은 Domain을 향한다.
- Controller는 **UseCase만** 호출
- Entity는 JPA 어노테이션 + 비즈니스 로직 함께 (분리하지 않음)
- Domain Service에 Repository 주입 금지 (순수 함수만)

## 각 레이어 책임

### Interfaces Layer (interfaces/api)

- HTTP 요청/응답 매핑
- 비즈니스 규칙 넣지 않음
- DTO ↔ Command/Response 변환
- HTTP 상태코드 결정
- `@Valid`로 입력 검증
- UseCase만 호출

### Application Layer (UseCase)

- **유스케이스 1개 = 클래스 1개**, `execute()` 메서드 하나
- `{행위}{도메인}UseCase` 네이밍 (예: `RegisterBrandUseCase`, `CreateOrderUseCase`)
- Repository를 통한 데이터 조회/저장
- DB 조회가 필요한 비즈니스 규칙 처리 (중복 검증 등)
- 여러 도메인 조합 (어셈블링)
- 트랜잭션 경계 (`@Transactional`)
- 순수 Domain만으로 처리하지 못하는 부분은 Domain Service에 위임

### Domain Layer (domain)

- **Entity**: JPA 어노테이션 + 비즈니스 로직 함께. private constructor + companion object factory.
- **VO**: 불변 값 객체, 도메인 규칙 내장.
- **Domain Service**: Repository 주입 없음, 순수 비즈니스 로직만. 여러 Entity/VO 협력.
- **Repository 인터페이스**: Domain Layer에 위치 (DIP).

### Infrastructure Layer (infrastructure)

- JpaRepository 구현체 (Repository 인터페이스를 직접 구현)
- 외부 기술 의존 (Redis, Kafka 등)

## 패키지 구성 (계층 + 도메인)

```
/interfaces/api/{domain}/     ← Controller, DTO
/application/{domain}/         ← UseCase, Command(입력 DTO), Info(출력 DTO)
/domain/{domain}/              ← Entity, VO, Domain Service, Repository(interface)
/infrastructure/{domain}/      ← JpaRepository
/support/                      ← PageResult, ApiResponse
/support/error/                ← Exception, ErrorCode
```

예시 (Brand 도메인):
```
domain/brand/Brand.kt                   ← Entity (JPA 어노테이션 + 비즈니스 로직)
domain/brand/BrandRepository.kt         ← Repository interface
infrastructure/brand/BrandJpaRepository.kt  ← Repository 인터페이스 직접 구현
application/brand/RegisterBrandUseCase.kt
application/brand/UpdateBrandUseCase.kt
application/brand/DeleteBrandUseCase.kt
application/brand/GetBrandUseCase.kt
application/brand/BrandCommand.kt       ← 입력 DTO
application/brand/BrandInfo.kt          ← 출력 DTO
interfaces/api/brand/BrandV1Controller.kt
interfaces/api/brand/BrandResponse.kt
```

## ID 참조 원칙

- 모든 엔티티는 다른 엔티티를 직접 참조하지 않고 **ID(Long)만 보유**
- 다른 도메인 Entity **타입을 파라미터로 받는 것도 금지** → 원시 타입(Long, String)으로 전달
- `@OneToMany`, `@ManyToOne`, `@ManyToMany` 사용하지 않음
- 연관 데이터가 필요하면 UseCase에서 각 Repository를 호출하여 조합

## DTO / Command / Response

- Controller에서 Request DTO를 받아 UseCase에 전달
- UseCase는 도메인 객체로 로직 실행
- Response DTO는 Controller에서 Info 객체로부터 생성
- DTO의 `from()`은 단순 매핑만 (변환 로직 금지)

## 트랜잭션 경계

- ArgumentResolver는 트랜잭션 밖에서 실행
- Controller → UseCase로 Entity 대신 **ID 전달**
- UseCase가 `@Transactional` 내에서 Entity 조회

## Repository 설계 원칙

- Repository 인터페이스는 **Domain Layer**에 위치
- Repository 인터페이스에 **Spring Data 타입을 노출하지 않음**: `Pageable`, `Page<T>`, `Sort` 사용 금지
- 페이지네이션은 `page: Int, size: Int` 파라미터로 받고 `PageResult<T>`로 반환
- 조회 조건은 도메인 자체 타입 사용: `ProductSearchCondition`, `OrderSearchCondition`
- Infrastructure 구현체에서 Spring Data 타입으로 변환하여 처리
- **메서드 네이밍은 비즈니스 언어 사용** (인프라 구현 방식 노출 금지)

```kotlin
// ✅ 비즈니스 언어
interface BrandRepository {
    fun findAllActive(): List<Brand>
    fun findActiveByIdOrNull(id: Long): Brand?
}

// ❌ 인프라 구현 노출
interface BrandRepository {
    fun findAllByDeletedAtIsNull(): List<Brand>
}
```

## 크로스 도메인 조회 전략

**기본 원칙**: 처음에는 **애플리케이션 조합**으로 시작. 성능 문제가 측정된 이후에 Infrastructure JOIN으로 전환.

### 애플리케이션 조합 (기본)

- UseCase에서 각 Repository를 직접 호출해서 조합
- 각 Repository는 자기 도메인 객체만 반환

```kotlin
// ✅ 각 Repository는 자기 도메인만 반환
class GetProductListUseCase(
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
)

// ❌ Repository가 다른 도메인 객체를 반환하면 안 됨
interface LikeRepository {
    fun findLikedProducts(userId: Long): List<Product>  // 금지
}
```

### Infrastructure JOIN (성능 필요 시)

- Domain Layer에 조회 전용 Repository 인터페이스 정의 (`{Domain}QueryRepository`)
- JOIN 구현은 Infrastructure Layer의 구현체에만 존재
- 인터페이스에 JPA, QueryDSL 타입이 노출되면 안 됨

## 로직 배치 기준

| 상황 | 위치 | 이유 |
|------|------|------|
| 단일 Entity 혼자 판단 가능 | Entity 내부 메서드 | 자기 책임 |
| 여러 Entity 협력, DB 조회 없음 | Domain Service | 순수 도메인 협력 |
| DB 조회가 필요한 비즈니스 규칙 | UseCase | Repository는 UseCase 역할 |
| 두 도메인 이상 조합 | UseCase | 어셈블링은 UseCase 역할 |
| 비밀번호 길이/형식 | `Password` Value Object | 값 자체에 규칙 내장 |
| 재고는 0 미만 불가 | `Product.decreaseStock()` | 현실에서도 참인 규칙 |
| 브랜드명 중복 불가 | `RegisterBrandUseCase` | DB 조회 필요 |
| 브랜드 삭제 시 상품 중지 | `DeleteBrandUseCase` | 두 도메인 조합 |
| 주문 전수 검증 | `OrderValidator` (Domain Service) | 순수 함수, 여러 Entity 협력 |
