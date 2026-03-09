# 상품 샘플 데이터 생성 완료 ✅

## 데이터 요약

### 📊 통계
- **Brand**: 100개
- **Product**: 100,000개
- **SQL 파일 크기**: 21MB
- **총 INSERT 문**: 100,109줄

### 🔍 데이터 분포

#### 상품 상태 (Status)
- **ACTIVE**: 80,119개 (80.1%)
- **INACTIVE**: 19,881개 (19.9%)

#### 상품 가격 (Price)
- **저가 상품** (1,000~11,000원): ~30%
  - 예: 신발, 이어폰, 보조배터리 등
  
- **일반 상품** (50,000~150,000원): ~40%
  - 예: 스마트폰, 노트북, 태블릿 등
  
- **고가 상품** (100,000~500,000원): ~30%
  - 예: 카메라, 모니터, 게이밍기기 등

#### 상품 재고 (Stock)
- **재고 없음** (0): ~5%
- **적은 재고** (1~100): ~15%
- **보통 재고** (100~1,100): ~40%
- **많은 재고** (1,000~10,000): ~40%

#### 상품 좋아요 (Like Count)
- **좋아요 없음** (0): ~40%
- **적은 좋아요** (1~100): ~30%
- **중간 좋아요** (100~600): ~20%
- **많은 좋아요** (600~1,000): ~10%

#### 상품 카테고리
20가지 카테고리가 랜덤하게 분포:
- 스마트폰, 노트북, 태블릿, 이어폰
- 카메라, 시계, 신발, 가방
- 의류, 액세서리, 가전제품, 게이밍기기
- 헤드폰, 마우스, 키보드, 모니터
- 프린터, 스캐너, 라우터, 보조배터리

### 📝 사용 방법

1. **MySQL 접속**
```bash
mysql -u root -p loopers
```

2. **SQL 스크립트 실행**
```sql
source sample-data-insert.sql;
```

3. **데이터 확인**
```sql
-- 전체 상품 수
SELECT COUNT(*) FROM products;

-- 브랜드별 상품 수
SELECT b.name, COUNT(p.id) as product_count
FROM products p
JOIN brands b ON p.brand_id = b.id
GROUP BY b.id
ORDER BY product_count DESC
LIMIT 10;

-- 상태별 통계
SELECT status, COUNT(*) as count, 
       ROUND(AVG(price), 2) as avg_price,
       AVG(stock) as avg_stock,
       AVG(like_count) as avg_likes
FROM products
GROUP BY status;

-- 가격 범위별 통계
SELECT 
  CASE 
    WHEN price < 11000 THEN '저가 (1K~11K)'
    WHEN price < 150000 THEN '일반 (50K~150K)'
    ELSE '고가 (100K~500K)'
  END as price_range,
  COUNT(*) as count,
  ROUND(AVG(price), 2) as avg_price,
  ROUND(AVG(stock), 2) as avg_stock
FROM products
GROUP BY price_range;
```

### ⚡ 성능 최적화 팁

1. **대량 INSERT 성능 향상**
```sql
SET FOREIGN_KEY_CHECKS=0;
SET UNIQUE_CHECKS=0;
-- SQL 스크립트 실행
SET FOREIGN_KEY_CHECKS=1;
SET UNIQUE_CHECKS=1;
```

2. **인덱스 확인**
```sql
-- products 테이블의 인덱스 확인
SHOW INDEXES FROM products;

-- 검색 최적화를 위한 복합 인덱스
CREATE INDEX idx_brand_status ON products(brand_id, status);
CREATE INDEX idx_price_stock ON products(price, stock);
```

3. **쿼리 성능 테스트**
```sql
-- 실행 계획 확인
EXPLAIN SELECT * FROM products 
WHERE brand_id = 10 AND status = 'ACTIVE' 
ORDER BY like_count DESC 
LIMIT 20;
```

### 📂 파일 위치
- SQL 스크립트: `./sample-data-insert.sql`
- 생성 스크립트: `./generate-sample-data.sh`
- 이 문서: `./data-summary.md`
