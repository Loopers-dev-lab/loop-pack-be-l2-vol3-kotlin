# 상품 목록 조회 성능 최적화 - 인덱스 설계

## 목표

상품 목록 API에서 **brandId 기반 필터링** + **다중 정렬 옵션(좋아요순, 생성순, 가격순)** 을 지원하면서 100만~1000만 건 데이터 규모에서 **O(log n) 조회 성능** 보장

---

## 현황 분석

### 문제점

1. **좋아요 순 정렬 미지원**
   - ProductRepositoryImpl에서 `like_count` 정렬 미구현
   - `like_count` 컬럼에 인덱스 없음

2. **정렬 컬럼별 인덱스 부재**
   - 현재: `idx_brand_status`, `idx_status`, `idx_price`, `idx_created_at` (단순 인덱스)
   - 필터링 + 정렬 조합을 위한 복합 인덱스 없음
   - 결과: 필터링 후 filesort 발생 → 성능 저하

### 조회 패턴 분석

```kotlin
// ProductRepositoryImpl에서 실제 사용되는 쿼리
fun findWithPaging(brandId: Long?, pageable: Pageable): Page<Product>
fun findActiveProductsWithPaging(brandId: Long?, pageable: Pageable): Page<Product>

// 정렬 옵션 (현재)
"createdAt" → idx_created_at 사용 (단순 인덱스)
"price"     → idx_price 사용 (단순 인덱스)

// 정렬 옵션 (신규)
"likeCount" → 인덱스 없음 (filesort) ❌
```

---

## 설계 방안

### 선택 이유: 복합 인덱스 (Composite Index)

**읽기 90% 이상** → 조회 성능 최우선 최적화

#### 인덱스 구조: (필터 컬럼, 정렬 컬럼)

```sql
-- 1. Brand 필터링 (3개)
CREATE INDEX idx_brand_like ON products(brand_id, like_count DESC);
CREATE INDEX idx_brand_created ON products(brand_id, created_at DESC);
CREATE INDEX idx_brand_price ON products(brand_id, price);

-- 2. Status 필터링 (3개)
CREATE INDEX idx_status_like ON products(status, like_count DESC);
CREATE INDEX idx_status_created ON products(status, created_at DESC);
CREATE INDEX idx_status_price ON products(status, price);
```

### 설계 근거

#### 1. MySQL B-tree 인덱스 특성

```
인덱스: (brand_id, like_count DESC)

쿼리 실행:
SELECT * FROM products
WHERE brand_id = 1
ORDER BY like_count DESC

동작:
1. brand_id=1인 데이터 위치 찾기 (첫 번째 컬럼)
2. like_count 내림차순으로 이미 정렬됨 (두 번째 컬럼)
3. 추가 정렬 필요 없음 (filesort 미발생) ✅
```

#### 2. 필터 기준: 2가지

- **Brand 필터링**: findWithPaging 메서드에서 주로 사용
- **Status 필터링**: findActiveProductsWithPaging에서 사용

#### 3. 정렬 옵션: 3가지

- **like_count DESC**: 인기도 기반 (신규)
- **created_at DESC**: 신상품 기반 (기존)
- **price ASC/DESC**: 가격대 기반 (기존)

#### 4. deletedAt 제외 이유

- 모든 쿼리에서 `deleted_at IS NULL` 공통 조건
- 값이 항상 NULL → 인덱스 선택도 동일
- 인덱스에 포함하면 크기만 증가 (+15~20%)
- WHERE 절에서 필터링하는 것이 효율적

---

## 성능 예상

### 100만 건 기준

| 쿼리 | 이전 | 이후 | 개선도 |
|------|------|------|-------|
| `brandId=1, sort=likeCount` | 500ms (filesort) | 5~10ms (index scan) | **-98%** ✅ |
| `brandId=1, sort=createdAt` | 200ms | 5~10ms | **-97%** |
| `status=ACTIVE, sort=likeCount` | 800ms | 10~20ms | **-98%** |

### 쓰기 성능 영향

| 작업 | 영향도 | 비고 |
|------|--------|------|
| INSERT | +20~30% | 6개 인덱스 모두 업데이트 |
| UPDATE (like_count) | +15% | 2개 인덱스만 업데이트 |
| UPDATE (price) | +15% | 2개 인덱스만 업데이트 |

**결론**: 읽기 90% → 쓰기 비용 무시할 수 있는 수준 ✅

---

## 구현 계획

### Phase 1: 인덱스 생성

```sql
-- 기존 인덱스 확인
SHOW INDEX FROM products;

-- 신규 인덱스 생성 (6개)
CREATE INDEX idx_brand_like ON products(brand_id, like_count DESC);
CREATE INDEX idx_brand_created ON products(brand_id, created_at DESC);
CREATE INDEX idx_brand_price ON products(brand_id, price);

CREATE INDEX idx_status_like ON products(status, like_count DESC);
CREATE INDEX idx_status_created ON products(status, created_at DESC);
CREATE INDEX idx_status_price ON products(status, price);

-- 불필요한 기존 인덱스 검토 및 삭제
-- idx_created_at, idx_price 등은 이제 복합 인덱스로 대체됨
```

### Phase 2: 코드 변경

#### 1. ProductRepositoryImpl에 like_count 정렬 지원

```kotlin
// findWithPaging, findActiveProductsWithPaging 수정
val orders = pageable.sort.mapNotNull { order ->
    val path = when (order.property) {
        "createdAt" -> qProduct.createdAt
        "price" -> qProduct.price
        "likeCount" -> qProduct.likeCount  // ← 추가
        else -> null
    }
    // ...
}
```

#### 2. ProductSortOption 추가

```kotlin
enum class ProductSortOption {
    LIKE_COUNT,
    CREATED_AT,
    PRICE,
}
```

#### 3. API 명세 업데이트

```kotlin
// ProductV1ApiSpec
fun getProducts(
    brandId: Long?,
    page: Int,
    size: Int,
    sort: String?  // "likeCount", "createdAt", "price"
): ApiResponse<PageResponse<ProductInfo>>
```

### Phase 3: 테스트

#### 단위 테스트
- 각 정렬 옵션별 쿼리 검증
- Repository 메서드 테스트

#### 성능 테스트
- 100만 건 데이터: 응답 시간 < 50ms
- 1000만 건 데이터: 응답 시간 < 100ms
- INDEX EXPLAIN 분석: filesort 없어야 함

#### E2E 테스트
- API 엔드포인트 동작 확인
- 다양한 필터링 + 정렬 조합 검증

---

## 마이그레이션 전략

### 무중단 배포 (Zero-Downtime)

```
Step 1: 신규 인덱스 생성 (온라인 가능, 5~10분 소요)
├─ ALTER TABLE ... 또는 CREATE INDEX CONCURRENTLY
└─ 기존 쿼리는 계속 동작

Step 2: 애플리케이션 배포
├─ like_count 정렬 코드 추가
└─ 신규 인덱스 사용 시작

Step 3: 기존 인덱스 제거 (선택사항, 1주일 후)
├─ 모니터링 후 불필요한 단순 인덱스 삭제
└─ 디스크 공간 정리
```

---

## 확장성

### 1000만 건 이후 고려사항

| 규모 | 인덱스 전략 | 추가 고려 |
|------|----------|---------|
| **100만** | 복합 인덱스 ✅ | - |
| **1000만** | 복합 인덱스 ✅ | 모니터링 |
| **1억** | 복합 인덱스 + 파티셔닝 | 테이블 파티셔닝 검토 |

---

## 검증 체크리스트

- [ ] 인덱스 생성 SQL 검증
- [ ] EXPLAIN 분석: filesort 없음 확인
- [ ] 성능 테스트 통과 (응답 시간 기준)
- [ ] E2E 테스트 통과
- [ ] 배포 전 백업 확보
- [ ] 모니터링 설정 (쿼리 성능, 인덱스 사용률)

---

## 참고

- MySQL B-tree 인덱스 원리: 복합 인덱스의 첫 컬럼은 필터링, 다음 컬럼은 정렬에 사용
- InnoDB 스토리지 엔진 기준
- EXPLAIN 명령어로 쿼리 계획 확인 필수
