# 상품 조회 성능 테스트 리포트

## 📊 테스트 개요

**목적**: 10만개 상품 데이터에서의 조회 성능 검증

**테스트 항목**:
- 기본 조회 성능 (COUNT, 단건, 페이징)
- 페이징 성능 (다양한 오프셋)
- 필터링 성능 (상태, 브랜드)
- 정렬 성능 (가격순, 생성일순)
- 대량 동시 조회 시뮬레이션

**테스트 환경**:
- 데이터: 100,000개 상품 + 100개 브랜드
- 프레임워크: Spring Boot + JPA + QueryDSL
- 데이터베이스: Testcontainers MySQL
- 프로필: test

---

## 🎯 성능 기준 (SLA)

| 항목 | 기준 | 우선순위 |
|------|------|---------|
| 단건 조회 | < 100ms | 🔴 Critical |
| 첫 페이지 조회 | < 500ms | 🔴 Critical |
| 중간 페이지 조회 | < 800ms | 🟡 Important |
| 뒷 페이지 조회 | < 1,500ms | 🟡 Important |
| 전체 개수 조회 | < 2,000ms | 🟡 Important |
| 필터링 조회 | < 800ms | 🟡 Important |
| 정렬 조회 | < 1,000ms | 🟡 Important |
| 복합 쿼리 | < 2,000ms | 🟢 Nice-to-have |

---

## 📈 테스트 결과

> 테스트 실행 중... 결과가 업데이트됩니다.

### 1. 기본 조회 성능 (BasicQueryPerformance)

#### COUNT 쿼리
```
⏱️  전체 상품 개수 조회: [ms]
기준: < 2,000ms ✅/❌
```

#### 첫 페이지 조회
```
⏱️  첫 페이지 조회: [ms]
조회된 상품 수: 20개
기준: < 500ms ✅/❌
```

#### 단건 조회
```
⏱️  단건 조회 (ID=1): [ms]
기준: < 100ms ✅/❌
```

### 2. 페이징 성능 (PagingPerformance)

#### 중간 페이지 (Page 50, Size 20)
```
⏱️  중간 페이지 조회 (Page 50): [ms]
기준: < 800ms ✅/❌
분석: OFFSET 1,000 정도의 성능
```

#### 뒷 페이지 (Page 2,500, Size 20)
```
⏱️  뒷 페이지 조회 (Page 2,500): [ms]
기준: < 1,500ms ✅/❌
분석: OFFSET 50,000대 성능 측정
```

#### 대량 페이징 (Page Size 100)
```
⏱️  대량 조회 (Page Size=100): [ms]
기준: < 800ms ✅/❌
분석: 한 번에 큰 양의 데이터 조회
```

#### 연속 페이징 (10 페이지)
```
⏱️  연속 페이징 (10 페이지): [ms]
평균: [ms]/page
최대: [ms]
기준: < 10,000ms (전체) ✅/❌
분석: 실제 사용 시나리오
```

### 3. 필터링 성능 (FilteringPerformance)

#### 활성 상품만 조회
```
⏱️  활성 상품 조회: [ms]
조회된 활성 상품: ~80,000개
기준: < 800ms ✅/❌
분석: WHERE 조건 필터링 성능
```

#### 브랜드별 필터링
```
⏱️  브랜드별 필터링 조회: [ms]
브랜드 5의 총 상품 수: [개]개
기준: < 500ms ✅/❌
분석: FK 기반 필터링
```

#### 복합 필터링 (활성 + 브랜드)
```
⏱️  복합 필터링 조회: [ms]
브랜드 10의 활성 상품 수: [개]개
기준: < 600ms ✅/❌
분석: AND 조건 복합 필터링
```

### 4. 정렬 성능 (SortingPerformance)

#### 가격순 정렬 (오름)
```
⏱️  가격순 정렬 (오름): [ms]
기준: < 1,000ms ✅/❌
분석: ORDER BY price ASC
```

#### 가격순 정렬 (내림)
```
⏱️  가격순 정렬 (내림): [ms]
기준: < 1,000ms ✅/❌
분석: ORDER BY price DESC
```

#### 생성일순 정렬
```
⏱️  생성일순 정렬: [ms]
기준: < 800ms ✅/❌
분석: ORDER BY createdAt DESC
```

### 5. 대량 동시 조회 시뮬레이션 (ConcurrentQueryPerformance)

#### 순차 조회 (10회)
```
⏱️  순차 조회 (10회):
평균: [ms]
최대: [ms]
기준: 평균 < 500ms, 최대 < 1,000ms ✅/❌

각 조회:
  Page 0: [ms]
  Page 1: [ms]
  ...
  Page 9: [ms]
```

#### 복합 쿼리 패턴 (3가지)
```
⏱️  복합 쿼리 패턴 (3가지): [ms]
기준: < 2,000ms ✅/❌
분석:
  1. 기본 조회
  2. 정렬 + 필터링
  3. 깊은 페이징
```

### 6. 데이터 검증 (DataValidation)

```
📊 로드된 데이터 통계:
✅ 전체 상품: [개]개
✅ 활성 상품: [개]개 ([%]%)
✅ 비활성 상품: [개]개 ([%]%)
✅ 평균 가격: [원]원
✅ 평균 재고: [개]개
✅ 평균 좋아요: [개]개
```

---

## 🔍 성능 분석

### 빠른 항목 ⚡

| 항목 | 실측 | 기준 | 여유 |
|------|------|------|------|
| 단건 조회 | - | 100ms | - |
| 첫 페이지 | - | 500ms | - |
| 브랜드 필터링 | - | 500ms | - |

### 확인 필요 항목 ⚠️

| 항목 | 실측 | 기준 | 상태 |
|------|------|------|------|
| 뒷 페이지 조회 | - | 1,500ms | - |
| 정렬 성능 | - | 1,000ms | - |

---

## 💡 개선 권장사항

### 1. 인덱스 최적화

**추천**:
```sql
-- 상태 필터링 빠르게
CREATE INDEX idx_status ON products(status);

-- 브랜드별 조회 빠르게
CREATE INDEX idx_brand_id ON products(brand_id);

-- 가격 정렬 빠르게
CREATE INDEX idx_price ON products(price);

-- 복합 조건 빠르게
CREATE INDEX idx_brand_status ON products(brand_id, status);
```

### 2. 쿼리 최적화

**현재**: JOIN + OFFSET 사용
```kotlin
.innerJoin(qProduct.brand).fetchJoin()  // Eager loading
.offset(pageable.offset)
.limit(pageable.pageSize.toLong())
```

**고려사항**:
- OFFSET이 크면 느려짐 (seek 방식 고려)
- Fetch join으로 N+1 방지
- 정렬이 있으면 인덱스 활용

### 3. 캐싱 전략

**추천**:
- 상품 상세: Redis 캐시 (TTL: 1시간)
- 브랜드 목록: 로컬 캐시 (TTL: 30분)
- 인기 상품: 정기적 갱신

---

## 📋 체크리스트

- [ ] 모든 테스트 통과 확인
- [ ] 성능 기준 충족 확인
- [ ] 느린 쿼리 식별 및 개선
- [ ] 인덱스 생성 및 검증
- [ ] 부하 테스트 실행
- [ ] 프로덕션 배포 전 검증

---

## 🔗 참고

- 테스트 코드: `ProductPerformanceTest.kt`
- 생성된 데이터: `sample-data-insert.sql` (100,000개)
- 실행 방법: `./gradlew test --tests ProductPerformanceTest -Dspring.profiles.active=test`

---

**마지막 업데이트**: [테스트 실행 시간]
