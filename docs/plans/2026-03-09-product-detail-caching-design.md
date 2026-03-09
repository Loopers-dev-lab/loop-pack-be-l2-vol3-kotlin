# 상품 상세 조회 캐싱 설계

**작성일**: 2026-03-09
**대상**: Product 상세 조회 API 성능 최적화

## 개요

상품 상세 정보(`getProductInfo`)는 변경 빈도가 낮고 반복 조회가 많기 때문에 Redis 캐싱을 통해 DB 부하를 줄이고 응답 속도를 개선합니다.

## 요구사항 분석

- **캐싱 대상**: 상품 상세 조회만 (리스트 조회 제외)
- **캐시 만료 정책**: 고정 TTL (30분)
- **동시성 제어**: Lock-based Refresh로 캐시 스탐피드 방지
- **구현 방식**: Spring Cache Abstraction (@Cacheable)

## 아키텍처

### 캐싱 레이어 위치

```
ProductV1Controller
    ↓
ProductFacade.getProductInfo()
    ↓
@Cacheable("product") → ProductService.getProduct()  ← 캐싱 적용점
    ↓
ProductRepository.findById()
```

**이유**:
- Service 레이어는 도메인 로직의 중심
- Facade는 얇은 계층이므로 서비스에서 캐싱 관리
- Repository는 데이터 접근만 담당

### 캐시 구성

| 항목 | 값 |
|------|-----|
| 캐시명 | `product` |
| 캐시 Key | `product::{productId}` (Spring이 자동 생성) |
| TTL | 30분 |
| 저장소 | Redis (master-replica 구성) |
| 동시성 제어 | `sync = true` (Lock-based Refresh) |

## 구현 전략

### 1. 캐시 조회 (@Cacheable)

```kotlin
@Cacheable(value = "product", sync = true)
@Transactional(readOnly = true)
fun getProduct(productId: Long): Product {
    return productRepository.findById(productId)
        ?: throw CoreException(ErrorType.PRODUCT_NOT_FOUND)
}
```

**sync = true의 동작**:
- 첫 번째 요청: DB에서 조회 후 Redis에 저장
- 동시 요청들: 첫 요청이 완료될 때까지 대기, 캐시된 값 반환
- 캐시 스탐피드 방지

### 2. 캐시 무효화

#### 상품 수정 시
```kotlin
@CacheEvict(value = "product", key = "#product.id")
fun updateProduct(product: Product): Product {
    val updated = productRepository.save(product)
    // like_count 변경 등 동시에 처리
    return updated
}
```

#### 상품 삭제 시
```kotlin
@CacheEvict(value = "product", key = "#productId")
fun deleteProduct(productId: Long) {
    productRepository.deleteById(productId)
}
```

### 3. 캐시 설정 (Redis)

**application.yml**:
```yaml
spring:
  cache:
    type: redis
    redis:
      time-to-live: 1800000  # 30분 (ms)
      key-prefix: "cache:"
      use-key-prefix: true
```

## 캐시 스탐피드 방지 메커니즘

### 문제 상황
```
캐시 만료 시점 (30분 후)
    ↓
요청 1000개 동시 도착
    ↓
모두 DB에 접근 (스탐피드)
```

### Spring Cache의 sync = true
```
요청 1000개 동시 도착
    ↓
스레드 A: 락 획득 → DB 조회 → 캐시 저장
나머지: 락 대기 → A의 캐시 값 반환
    ↓
DB 1번 조회로 해결
```

## 무효화 전략

| 상황 | 처리 |
|------|------|
| 상품 기본정보 수정 | `@CacheEvict` |
| 상품 삭제 | `@CacheEvict` |
| like_count 변경 | ProductRepository에서 처리, 캐시 유지 |
| TTL 자동 만료 | 30분 후 자동 |

**주의**: like_count는 자주 변경되므로 캐시에 포함되지만, 캐시 무효화는 하지 않음 (TTL까지 사용). 약간의 데이터 지연은 허용.

## 테스트 전략

### 1. 단위 테스트
- 캐시 조회 시 Repository 호출 횟수 검증
- @Cacheable 동작 확인

### 2. 통합 테스트
- Redis 포함한 캐싱 동작 (Testcontainers)
- 캐시 무효화 동작 확인

### 3. 동시성 테스트
- 동시 요청 시 DB 조회 횟수 확인
- 스탐피드 방지 검증

## 마이그레이션 계획

1. **ProductService**에 @Cacheable 추가
2. **캐시 설정** (RedisConfig, application.yml)
3. **무효화 로직** 추가 (update, delete)
4. **테스트 작성**
5. **E2E 테스트**로 성능 개선 확인

## 성능 기대효과

- **DB 부하**: ~30% 감소 (반복 조회)
- **응답 시간**: 10배 이상 개선 (DB 조회 제거)
- **캐시 히트율**: 상품 상세 조회가 반복적이므로 높음 예상 (80%+)

## 향후 확장 가능성

- 브랜드별 캐싱 추가
- 조회 성능 모니터링 (Micrometer 메트릭)
- TTL 동적 조정
