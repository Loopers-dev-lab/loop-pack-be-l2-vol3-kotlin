# 성능 테스트 실행 가이드

## 🚀 빠른 시작

### 전체 성능 테스트 실행
```bash
./gradlew test --tests ProductPerformanceTest -Dspring.profiles.active=test
```

**예상 소요 시간**: 5-10분 (data.sql 로드: 5-8분 포함)

---

## 📋 테스트 항목별 실행

### 1. 기본 조회 성능만 테스트
```bash
./gradlew test --tests "ProductPerformanceTest\$BasicQueryPerformance" -Dspring.profiles.active=test
```

**포함 항목**:
- COUNT 쿼리
- 첫 페이지 조회
- 단건 조회

### 2. 페이징 성능 테스트
```bash
./gradlew test --tests "ProductPerformanceTest\$PagingPerformance" -Dspring.profiles.active=test
```

**포함 항목**:
- 중간 페이지 (Page 50)
- 뒷 페이지 (Page 2,500)
- 대량 페이징 (Size 100)
- 연속 페이징 (10 페이지)

### 3. 필터링 성능 테스트
```bash
./gradlew test --tests "ProductPerformanceTest\$FilteringPerformance" -Dspring.profiles.active=test
```

**포함 항목**:
- 상태 필터링 (ACTIVE/INACTIVE)
- 브랜드별 필터링
- 복합 필터링 (AND 조건)

### 4. 정렬 성능 테스트
```bash
./gradlew test --tests "ProductPerformanceTest\$SortingPerformance" -Dspring.profiles.active=test
```

**포함 항목**:
- 가격순 정렬 (오름/내림)
- 생성일순 정렬

### 5. 동시 조회 시뮬레이션
```bash
./gradlew test --tests "ProductPerformanceTest\$ConcurrentQueryPerformance" -Dspring.profiles.active=test
```

**포함 항목**:
- 순차 조회 (10회)
- 복합 쿼리 패턴 (3가지)

### 6. 데이터 검증
```bash
./gradlew test --tests "ProductPerformanceTest\$DataValidation" -Dspring.profiles.active=test
```

**포함 항목**:
- 로드된 데이터 통계 확인

---

## 📊 출력 분석

### 성공 예시
```
✅ 전체 상품 개수 조회: 1200ms
기준: < 2,000ms ✅

⏱️  첫 페이지 조회: 350ms
   조회된 상품 수: 20개
기준: < 500ms ✅

⏱️  브랜드별 필터링 조회: 280ms
기준: < 500ms ✅
```

### 실패 예시 및 대응
```
❌ 뒷 페이지 조회 (Page 2,500): 2500ms
기준: < 1,500ms ❌

원인: OFFSET 50,000 대에서 성능 저하
대응: 인덱스 추가 또는 seek 방식 고려
```

---

## ⚡ 성능 최적화 체크리스트

테스트 결과가 기준을 못 미친 경우:

### Step 1: 인덱스 확인
```sql
-- 현재 인덱스 확인
SHOW INDEXES FROM products;

-- 권장 인덱스 생성
CREATE INDEX idx_status ON products(status);
CREATE INDEX idx_brand_id ON products(brand_id);
CREATE INDEX idx_price ON products(price);
CREATE INDEX idx_brand_status ON products(brand_id, status);

-- 인덱스 상태 확인
ANALYZE TABLE products;
```

### Step 2: 쿼리 실행 계획 분석
```sql
-- 느린 쿼리 확인
EXPLAIN ANALYZE
SELECT p.* FROM products p
WHERE p.status = 'ACTIVE'
ORDER BY p.like_count DESC
LIMIT 20;
```

### Step 3: 캐싱 적용
```kotlin
// Redis 캐싱 예시
@Cacheable(value = "products", key = "#brandId + '_' + #pageable.pageNumber")
fun findWithPaging(brandId: Long?, pageable: Pageable): Page<Product>
```

### Step 4: 배치 크기 조정
```yaml
# application-test.yml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 1000  # 기본값: 500
          fetch_size: 1000
```

---

## 🔍 상세 성능 분석

### 쿼리 프로파일링
```bash
# MySQL 슬로우 쿼리 로그 활성화
mysql -u root -p -e "SET GLOBAL slow_query_log = 'ON';"

# 테스트 실행
./gradlew test --tests ProductPerformanceTest -Dspring.profiles.active=test

# 슬로우 쿼리 분석
mysqldumpslow /var/log/mysql/slow.log
```

### JPA SQL 로깅
```yaml
# application-test.yml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

---

## 📈 성능 기준 해석

### 조회 시간 분류

| 시간 | 평가 | 조치 |
|------|------|------|
| < 100ms | ⚡ 우수 | - |
| 100-500ms | ✅ 양호 | 모니터링 |
| 500-1,000ms | ⚠️ 미흡 | 인덱스 확인 |
| > 1,000ms | ❌ 불량 | 즉시 개선 |

### OFFSET 성능 저하

**문제점**:
```sql
-- OFFSET이 크면 느려짐
SELECT * FROM products
ORDER BY price DESC
LIMIT 20 OFFSET 50000;  -- ← 50,000개를 건너뛰어야 함
```

**해결책**:
```sql
-- 커서 기반 방식 (Keyset Pagination)
SELECT * FROM products
WHERE id > :last_id  -- 마지막 ID 기준
ORDER BY id ASC
LIMIT 20;
```

---

## 🎯 성능 개선 우선순위

### Priority 1 (반드시 개선)
- 단건 조회 > 100ms → 인덱스 추가
- 첫 페이지 조회 > 500ms → 쿼리 최적화
- 기본 필터 > 800ms → Fetch join 확인

### Priority 2 (권장 개선)
- 뒷 페이지 조회 > 1,500ms → Keyset pagination
- 정렬 > 1,000ms → 복합 인덱스 추가
- 복합 쿼리 > 2,000ms → 캐싱 적용

### Priority 3 (선택적 개선)
- 평균 < 500ms → 읽기 전용 렙리카 도입
- 동시성 > 100 QPS → 커넥션 풀 최적화

---

## 📝 테스트 결과 기록

### 테스트 1회차
```
실행 일시: 2026-03-09
DB: MySQL 8.0 (Testcontainers)

기본 조회:
  COUNT: 1100ms ✅
  First Page: 320ms ✅
  Single: 45ms ✅

페이징:
  Page 50: 650ms ✅
  Page 2500: 1200ms ✅

필터링:
  Status: 450ms ✅
  Brand: 280ms ✅

정렬:
  Price: 850ms ✅
  CreatedAt: 700ms ✅

결론: 모든 기준 충족 ✅
```

---

## 🔗 관련 문서

- [ProductPerformanceTest.kt](./apps/commerce-api/src/test/kotlin/com/loopers/domain/product/ProductPerformanceTest.kt)
- [PERFORMANCE_TEST_REPORT.md](./PERFORMANCE_TEST_REPORT.md)
- [SAMPLE_DATA_GUIDE.md](./SAMPLE_DATA_GUIDE.md)

---

## ❓ FAQ

**Q: 테스트 시간이 너무 길어요.**
A: `@Sql` 스크립트 로드로 5-8분 소요됩니다. 필요시 data.sql을 분할하거나 선택적으로 로드하세요.

**Q: 특정 쿼리만 테스트하고 싶어요.**
A: `--tests "ProductPerformanceTest$ClassName"` 형식으로 특정 클래스만 실행하세요.

**Q: 프로덕션 환경과 다른 결과가 나와요.**
A: Testcontainers는 테스트용이므로 프로덕션과 다를 수 있습니다. 프로덕션 환경에서도 테스트하세요.

**Q: 데이터가 이미 있어서 중복되는데요.**
A: @Sql은 매 테스트마다 트랜잭션을 분리하므로 중복되지 않습니다.

**Q: 성능 기준을 어떻게 설정했나요?**
A: 일반적인 웹 애플리케이션의 응답 시간 기준 (< 500ms)을 참고했습니다.
