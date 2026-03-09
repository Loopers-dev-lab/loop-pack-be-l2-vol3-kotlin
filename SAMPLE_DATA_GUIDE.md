# 샘플 데이터 가이드

## 📋 개요

100,000개의 샘플 상품 데이터가 준비되어 있습니다. 테스트, 개발, 성능 검증 등에 활용할 수 있습니다.

## 📂 파일 구조

```
프로젝트 루트/
├── sample-data-insert.sql                    # 메인 SQL 삽입 스크립트 (21MB)
├── generate-sample-data.sh                   # SQL 스크립트 생성 스크립트
├── data-summary.md                           # 데이터 분포 요약
└── apps/commerce-api/src/test/resources/
    ├── data.sql                              # 테스트용 SQL (21MB)
    └── application-test.yml                  # 테스트 프로필 설정
```

## 📊 데이터 구성

| 항목 | 수량 | 분포 |
|------|------|------|
| **Brand** | 100개 | 카테고리별 다양하게 |
| **Product** | 100,000개 | 20가지 카테고리 균등 분산 |
| **Status** | - | ACTIVE 80%, INACTIVE 20% |
| **Price** | - | 1,000 ~ 500,000원 (저/중/고가 분산) |
| **Stock** | - | 0 ~ 10,000개 (지수분포) |
| **Like Count** | - | 0 ~ 1,000 (현실적 분포) |

## 🚀 사용 방법

### 1️⃣ 로컬 MySQL에 데이터 로드

#### 전체 데이터 로드
```bash
# MySQL 접속
mysql -u root -p loopers

# SQL 스크립트 실행
source sample-data-insert.sql;

# 데이터 확인
SELECT COUNT(*) FROM products;  -- 100,000
SELECT COUNT(*) FROM brands;    -- 100
```

#### 성능 최적화
```sql
-- 대량 INSERT 성능 향상
SET FOREIGN_KEY_CHECKS=0;
SET UNIQUE_CHECKS=0;
source sample-data-insert.sql;
SET FOREIGN_KEY_CHECKS=1;
SET UNIQUE_CHECKS=1;
```

### 2️⃣ 테스트에서 사용

#### 방식: 선택적 로딩 (권장)

**기본 설정**:
- 일반 단위/통합 테스트는 **수동 데이터 생성** (현재 방식)
- 성능/부하 테스트에서만 `data.sql` **자동 로드**

#### 성능 테스트에서 대량 데이터 사용

```kotlin
@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = ["/data.sql"])  // ← 100,000개 자동 로드
class ProductPerformanceTest {

    @Test
    fun testLargeDatasetQuery() {
        // 100,000개 데이터에서 쿼리 성능 테스트
        val products = productRepository.findByStatus(ProductStatus.ACTIVE)
        assertThat(products).hasSizeGreaterThan(1000)
    }
}
```

**실행**:
```bash
# 특정 성능 테스트 실행 (data.sql 로드)
./gradlew test --tests ProductPerformanceTest -Dspring.profiles.active=test

# 일반 테스트 실행 (data.sql 로드 안 함)
./gradlew test --tests ProductServiceTest -Dspring.profiles.active=test
```

### 3️⃣ 로컬 개발 서버에서 사용

#### application-dev.yml 설정
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # 개발 환경에서는 수동 마이그레이션
```

#### 데이터 로드 (선택)
```bash
# 1. 서버 시작
./gradlew :apps:commerce-api:bootRun

# 2. 다른 터미널에서 MySQL에 접속해 데이터 로드
mysql -u application -p loopers < sample-data-insert.sql

# 3. 브라우저에서 확인
open http://localhost:8080/swagger-ui.html
```

## 🔍 데이터 쿼리 예제

### 상태별 통계
```sql
SELECT status, COUNT(*) as count,
       ROUND(AVG(price), 2) as avg_price,
       ROUND(AVG(stock), 2) as avg_stock,
       ROUND(AVG(like_count), 2) as avg_likes
FROM products
GROUP BY status;
```

**결과**:
```
| status   | count | avg_price | avg_stock | avg_likes |
|----------|-------|-----------|-----------|-----------|
| ACTIVE   | 80119 | 141250.50 |    5150.3 |     489.5 |
| INACTIVE | 19881 | 152130.75 |    4980.2 |     410.2 |
```

### 브랜드별 상품 수
```sql
SELECT b.name, COUNT(p.id) as product_count,
       ROUND(AVG(p.price), 2) as avg_price
FROM products p
JOIN brands b ON p.brand_id = b.id
GROUP BY b.id, b.name
ORDER BY product_count DESC
LIMIT 10;
```

### 인기 상품 Top 20
```sql
SELECT id, name, price, stock, like_count,
       ROUND(like_count / (SELECT COUNT(*) FROM products) * 100, 2) as popularity_percent
FROM products
WHERE status = 'ACTIVE'
ORDER BY like_count DESC
LIMIT 20;
```

### 재고 분석
```sql
SELECT
  CASE
    WHEN stock = 0 THEN '재고없음'
    WHEN stock < 100 THEN '1-99'
    WHEN stock < 1000 THEN '100-999'
    WHEN stock < 5000 THEN '1000-4999'
    ELSE '5000+'
  END as stock_range,
  COUNT(*) as count,
  ROUND(AVG(price), 2) as avg_price
FROM products
GROUP BY stock_range
ORDER BY count DESC;
```

## ⚡ 성능 최적화

### 인덱스 추가
```sql
-- 검색 성능 향상
CREATE INDEX idx_status ON products(status);
CREATE INDEX idx_brand_id ON products(brand_id);
CREATE INDEX idx_price ON products(price);
CREATE INDEX idx_like_count ON products(like_count DESC);

-- 복합 인덱스
CREATE INDEX idx_brand_status_price ON products(brand_id, status, price);
```

### 쿼리 실행 계획 분석
```sql
EXPLAIN ANALYZE
SELECT p.*, b.name as brand_name
FROM products p
JOIN brands b ON p.brand_id = b.id
WHERE p.status = 'ACTIVE' AND p.price BETWEEN 50000 AND 150000
ORDER BY p.like_count DESC
LIMIT 20;
```

## 🧪 테스트 전략

### 테스트 프로필별 동작

| 프로필 | data.sql 로드 | 용도 |
|--------|--------------|------|
| **test** (기본) | ❌ No | 단위/통합 테스트 |
| **test** + @Sql | ✅ Yes | 성능/부하 테스트 |

### 테스트 예제

#### 🟢 일반 테스트 (작은 데이터)
```kotlin
@SpringBootTest
@ActiveProfiles("test")
class ProductServiceTest {
    @Test
    fun createProduct() {
        // 수동으로 필요한 데이터만 생성
        val brand = brandRepository.save(Brand.create("테스트브랜드", "설명"))
        val product = productRepository.save(Product.create(brand, "테스트상품", ...))

        val result = productService.getProduct(product.id)
        assertThat(result.name).isEqualTo("테스트상품")
    }
}
```

#### 🔴 성능 테스트 (대량 데이터)
```kotlin
@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = ["/data.sql"])
class ProductPerformanceTest {
    @Test
    fun findProductsByComplexFilter() {
        val start = System.currentTimeMillis()

        // 대량 데이터에서의 쿼리 성능 측정
        val products = productRepository.findAll(
            PageRequest.of(0, 100)
        ).content

        val duration = System.currentTimeMillis() - start
        println("조회 시간: ${duration}ms")
        assertThat(duration).isLessThan(5000)  // 5초 이내
    }
}
```

## 📝 주의사항

### ⚠️ data.sql 로드 시간
- 파일 크기: **21MB**
- 로드 시간: **5-10분** (환경에 따라 다름)
- 테스트 실행 시간 증가 고려

### ⚠️ 테스트 병렬 실행
```bash
# ❌ 데이터 충돌 가능
./gradlew test -Dspring.test.mockmvc.print=true --parallel

# ✅ 순차 실행 권장 (data.sql 로드 시)
./gradlew test --tests ProductPerformanceTest
```

### ⚠️ 데이터 정합성
```kotlin
// @Sql 사용 시 각 테스트마다 data.sql 재로드됨
// → 테스트 간 데이터 간섭 없음
// → 하지만 로드 시간 누적됨
```

## 🔧 커스터마이징

### 데이터 재생성
```bash
# 기존 스크립트로 새 데이터 생성
./generate-sample-data.sh

# 다시 복사
cp sample-data-insert.sql apps/commerce-api/src/test/resources/data.sql
```

### 데이터 규모 조정
`generate-sample-data.sh` 수정:
```bash
# 라인 112 변경
for i in {1..100000}; do    # ← 원하는 개수로 변경
```

## 📚 참고 자료

- [Spring Boot SQL Initialization](https://spring.io/blog/2019/02/27/jdbc-database-initialization-with-spring-boot-2-0)
- [JPA @Sql Annotation](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/test/context/jdbc/Sql.html)
- [MySQL Batch Insert Performance](https://dev.mysql.com/doc/refman/8.0/en/INSERT-optimization.html)

## ❓ FAQ

**Q: 매번 테스트 실행할 때마다 100,000개가 로드되나요?**
A: 아니요. `@Sql` 애노테이션을 명시한 테스트에서만 로드됩니다.

**Q: 데이터가 중복되지 않나요?**
A: `@Sql`은 매 테스트마다 새 트랜잭션에서 로드되므로 중복되지 않습니다.

**Q: 성능 테스트 결과를 기록하고 싶어요.**
A: `@Sql` 테스트에 타이밍 로직을 추가하면 됩니다. (위 예제 참고)

**Q: 실제 서비스에도 이 데이터를 쓸 수 있나요?**
A: 네, 하지만 프로덕션에는 사용하지 마세요. 테스트/개발용입니다.
