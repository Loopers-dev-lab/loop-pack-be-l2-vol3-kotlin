# 🚀 성능 테스트 빠른 시작

## 📋 1단계: 샘플 데이터 준비

### MySQL에 데이터 로드
```bash
# 터미널 1: Docker에서 MySQL 실행
docker-compose -f docker/infra-compose.yml up

# 터미널 2: 데이터 로드
mysql -u application -p loopers < sample-data-insert.sql
# 비밀번호: application

# 확인
mysql -u application -p loopers -e "SELECT COUNT(*) FROM products;"
# 결과: 100000
```

## 📊 2단계: 성능 테스트 실행

### 로컬 서버 시작
```bash
# 터미널 3
./gradlew :apps:commerce-api:bootRun -Dspring.profiles.active=dev
```

### 성능 테스트 (별도 터미널)
```bash
# 일반 테스트 (수동 데이터)
./gradlew test

# 성능 테스트 (향후 업데이트 예정)
# ./gradlew test --tests ProductPerformanceTest
```

## 🔍 3단계: 조회 성능 검증

### curl 또는 Postman으로 테스트
```bash
# 1. 첫 페이지 (Page 1)
curl "http://localhost:8080/api/products?page=0&size=20"

# 2. 중간 페이지 (Page 50)
curl "http://localhost:8080/api/products?page=50&size=20"

# 3. 뒷 페이지 (Page 2,500)
curl "http://localhost:8080/api/products?page=2500&size=20"

# 4. 정렬 (가격순)
curl "http://localhost:8080/api/products?page=0&size=20&sort=price,desc"

# 5. 필터링 (활성 상품)
curl "http://localhost:8080/api/products?page=0&size=20&status=ACTIVE"
```

### 응답 시간 측정
```bash
# time 명령으로 측정
time curl "http://localhost:8080/api/products?page=0&size=20"

# 예상 결과: real 0m0.XXXs (< 500ms)
```

## 📈 성능 기준

| 작업 | 기준 | 상태 |
|------|------|------|
| 첫 페이지 조회 | < 500ms | ✅ |
| 중간 페이지 | < 800ms | ✅ |
| 뒷 페이지 | < 1,500ms | ⚠️ |
| 정렬 조회 | < 1,000ms | ⚠️ |

## 🛠️ 성능 최적화

### 1. 인덱스 추가
```sql
-- MySQL에 접속
mysql -u application -p loopers

-- 인덱스 생성
CREATE INDEX idx_status ON products(status);
CREATE INDEX idx_brand_id ON products(brand_id);
CREATE INDEX idx_price ON products(price);

-- 통계 갱신
ANALYZE TABLE products;

-- 인덱스 확인
SHOW INDEXES FROM products;
```

### 2. 쿼리 성능 분석
```sql
-- 실행 계획 확인
EXPLAIN ANALYZE
SELECT * FROM products
WHERE status = 'ACTIVE'
ORDER BY like_count DESC
LIMIT 20;
```

## 📁 관련 파일

- `sample-data-insert.sql` - 100,000개 샘플 데이터
- `SAMPLE_DATA_GUIDE.md` - 샘플 데이터 상세 가이드
- `PERFORMANCE_TEST_GUIDE.md` - 성능 테스트 상세 가이드
- `ProductPerformanceTest.kt` - 성능 테스트 코드
- `RUN_PERFORMANCE_TEST.sh` - 대화식 테스트 실행 스크립트

## ✅ 체크리스트

- [ ] Docker MySQL 실행 (`docker-compose up`)
- [ ] 샘플 데이터 로드 (`sample-data-insert.sql`)
- [ ] 데이터 확인 (`SELECT COUNT(*)`)
- [ ] 로컬 서버 시작 (`bootRun`)
- [ ] curl로 기본 조회 테스트
- [ ] 응답 시간 측정
- [ ] 필요시 인덱스 추가
- [ ] 성능 기준 확인

## 🆘 트러블슈팅

### "MySQL 연결 실패"
```bash
# MySQL 확인
docker ps | grep mysql

# 로그 확인
docker-compose -f docker/infra-compose.yml logs mysql
```

### "데이터 로드가 느림"
```bash
# 대량 로드 최적화 (MySQL)
SET FOREIGN_KEY_CHECKS=0;
SET UNIQUE_CHECKS=0;
source sample-data-insert.sql;
SET FOREIGN_KEY_CHECKS=1;
SET UNIQUE_CHECKS=1;
```

### "응답이 1초 이상"
```bash
# 1. 인덱스 확인
SHOW INDEXES FROM products;

# 2. 쿼리 분석
EXPLAIN ANALYZE SELECT ...;

# 3. 통계 갱신
ANALYZE TABLE products;
```

## 📞 지원

문제가 발생하면:
1. `PERFORMANCE_TEST_GUIDE.md` 확인
2. MySQL 로그 확인 (`docker-compose logs`)
3. 애플리케이션 로그 확인 (gradle console)
