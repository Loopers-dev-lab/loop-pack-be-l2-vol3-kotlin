# 성능 테스트 데이터 생성 가이드

## 개요

이 스크립트는 성능 테스트용 대량 데이터를 생성합니다:
- **상품**: 100만 개
- **좋아요**: 100만 개
- **브랜드**: 100개
- **사용자**: 1,000개

## 사전 요구사항

1. MySQL이 실행 중이어야 합니다
2. `loopers` 데이터베이스가 생성되어 있어야 합니다
3. 테이블 스키마가 생성되어 있어야 합니다

## 사용 방법

### 1. 로컬 환경 (기본값 사용)

```bash
cd docker/test-data
./load-test-data.sh
```

### 2. 환경변수로 DB 정보 지정

```bash
MYSQL_HOST=localhost \
MYSQL_PORT=3306 \
MYSQL_USER=application \
MYSQL_PWD=application \
./load-test-data.sh
```

### 3. Docker 환경에서 실행

```bash
docker exec loopers-infra-mysql-1 bash < load-test-data.sh
```

## 소요 시간

- 브랜드 생성: < 1초
- 사용자 생성: < 1초
- 상품 생성: 약 5-10분 (100만개)
- 좋아요 생성: 약 5-10분 (100만개)
- 좋아요 개수 업데이트: 약 5-10분
- **전체**: 약 20-30분

## 진행 상황 모니터링

스크립트 실행 중에 다른 터미널에서 진행 상황을 확인할 수 있습니다:

```bash
# 상품 개수 확인
docker exec loopers-infra-mysql-1 mysql -uapplication -papplication loopers -e \
  "SELECT COUNT(*) as product_count FROM products WHERE deleted_at IS NULL;"

# 좋아요 개수 확인
docker exec loopers-infra-mysql-1 mysql -uapplication -papplication loopers -e \
  "SELECT COUNT(*) as like_count FROM product_likes WHERE deleted_at IS NULL;"
```

## 데이터 검증

생성 완료 후 데이터를 확인합니다:

```bash
docker exec loopers-infra-mysql-1 mysql -uapplication -papplication loopers << 'EOF'
SELECT
    (SELECT COUNT(*) FROM brands) as brand_count,
    (SELECT COUNT(*) FROM users) as user_count,
    (SELECT COUNT(*) FROM products WHERE deleted_at IS NULL) as product_count,
    (SELECT COUNT(*) FROM product_likes WHERE deleted_at IS NULL) as like_count;
EOF
```

예상 결과:
```
brand_count | user_count | product_count | like_count
100         | 1000       | 1000000       | 1000000
```

## 데이터 삭제 (옵션)

모든 테스트 데이터를 삭제하려면:

```bash
docker exec loopers-infra-mysql-1 mysql -uapplication -papplication loopers << 'EOF'
-- 좋아요 삭제
DELETE FROM product_likes;

-- 상품 삭제
DELETE FROM products;

-- 사용자 삭제
DELETE FROM users;

-- 브랜드 삭제
DELETE FROM brands;
EOF
```

## 성능 테스트 실행

데이터 생성이 완료되면 성능 테스트를 실행할 수 있습니다:

```bash
# perf 프로필로 ProductPerformanceTest 실행
./gradlew :apps:commerce-api:test --tests "ProductPerformanceTest" -DprofileActive=perf
```

## 트러블슈팅

### 1. "Access denied" 에러
- MySQL 사용자명과 비밀번호를 확인하세요
- 환경변수를 설정하세요: `MYSQL_USER`, `MYSQL_PWD`

### 2. "Unknown database" 에러
- `loopers` 데이터베이스가 생성되어 있는지 확인하세요
- 스키마가 마이그레이션되었는지 확인하세요

### 3. 느린 속도
- 시스템의 디스크 I/O 성능에 따라 시간이 다를 수 있습니다
- MySQL의 `max_allowed_packet` 설정을 확인하세요:
  ```bash
  mysql -uapplication -papplication -e "SHOW VARIABLES LIKE 'max_allowed_packet';"
  ```

### 4. 중복 데이터 방지
- 스크립트는 자동으로 `INSERT IGNORE`를 사용하여 중복을 방지합니다
- 스크립트를 여러 번 실행해도 안전합니다
