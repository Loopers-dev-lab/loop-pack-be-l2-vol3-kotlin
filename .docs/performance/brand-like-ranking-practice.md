# Brand Like Ranking Performance Practice

## 참고 문서 반영

`brand-like-ranking-architecture-review.txt`의 핵심 전제를 기준으로 다음 원칙으로 구현했다.

- **source of truth는 likes 테이블**
- **조회 최적화용 집계값은 products.like_count**
- **hot path는 brand filter + likes desc**
- **캐시는 eventual consistency를 허용하되, 상세/브랜드 목록은 즉시 무효화**

## AS-IS

### 상품 상세 조회
- `ProductUseCase.getById`
- 상품 조회 후 `LikeReader.countByProductId(productId)`로 좋아요 수를 매번 집계

### 상품 목록 조회
- 상품 전체/브랜드별 조회
- 브랜드를 별도 조회
- `LikeReader.countByProductIds(productIds)`로 좋아요 수를 다시 집계
- 애플리케이션 메모리에서 `sortedByDescending { likeCount }`

### 문제
- 10만 건 이상에서 목록 조회 시 **상품 수집 + 좋아요 집계 + 메모리 정렬** 비용이 한 번에 발생
- `likes(member_id, product_id)` 유니크 인덱스만으로는 `countByProductId`, `group by product_id` 최적화가 약함
- Redis 설정만 있고 실제 API 캐시는 없었음

## TO-BE

### 1) 비정규화
- `products.like_count` 컬럼 추가
- 좋아요 등록/취소 시 같은 트랜잭션에서 `products.like_count` 증감
- 읽기 경로는 likes aggregate 대신 `products.like_count` 사용

### 2) 인덱스
- `products(brand_id, like_count desc, id desc)`  
  → 브랜드 필터 + 좋아요순 정렬 hot path 최적화
- `products(like_count desc, id desc)`  
  → 전체 인기 상품 조회 최적화
- `products(brand_id, id desc)`  
  → 브랜드 필터 + 최신순 조회 최적화
- `products(brand_id, price, id desc)`  
  → 브랜드 필터 + 가격순 조회 최적화
- `likes(product_id)`  
  → 원본 좋아요 집계 / backfill / 검증 쿼리 최적화

### 3) 조회 구조
- 상품 목록은 QueryDSL로 정렬을 DB에 위임
- 상세/목록 모두 `Product.likeCount` 사용
- 애플리케이션 메모리 정렬 제거

### 4) Redis 캐시
- 상품 상세: `10분 TTL`
- 상품 목록: `3분 TTL`
- 캐시 키
  - 상세: `products:detail:v:{version}:product:{productId}`
  - 목록: `products:list:gv:{globalVersion}:bv:{brandVersion}:sort:{sortType}:brand:{brandId|all}`
- 무효화 전략
  - 좋아요/상품 변경: 상세 key 삭제 + 목록 version 증가
  - 브랜드명 변경/비활성화: 상세 version 증가 + 목록 version 증가
- Redis 실패 시 DB 조회로 fallback

## 정합성 계약

- **진실한 원천**: `likes`
- **조회용 집계값**: `products.like_count`
- **동기화 시점**: like register/remove 트랜잭션 내부
- **캐시 일관성 수준**:
  - 상세: 즉시 무효화
  - 목록: 버전 증가 기반 즉시 무효화 + TTL

## 수동 실습 방법

### 1. 인프라 실행
```bash
docker-compose -f ./docker/infra-compose.yml up -d
```

### 2. 10만 건 데이터 적재
```bash
mysql -h 127.0.0.1 -P 3306 -u application -papplication loopers < scripts/performance/seed_brand_like_ranking.sql
```

### 3. EXPLAIN ANALYZE 실행
```bash
mysql -h 127.0.0.1 -P 3306 -u application -papplication loopers < scripts/performance/explain_brand_like_ranking.sql
```

### 4. API 부하 실험
- `GET /api/v1/products?brandId={id}&sortType=LIKES_DESC`
- `GET /api/v1/products/{id}`
- k6 스크립트: `k6/scripts/product-ranking-hot-path.js`

```bash
k6 run k6/scripts/product-ranking-hot-path.js \
  -e BASE_URL=http://localhost:8080 \
  -e HOT_BRAND_IDS=1,2,3,4,5 \
  -e HOT_PRODUCT_IDS=1,2,3,4,5,6,7,8,9,10
```

## 다음 단계

- 현재 목록 API는 전체 반환이므로, 실서비스 수준에서는 **cursor pagination** 도입이 다음 우선순위다.
- Materialized View는 `top N` 랭킹이 완전히 고정된 read-heavy 환경일 때 추가 검토 가능하다.
