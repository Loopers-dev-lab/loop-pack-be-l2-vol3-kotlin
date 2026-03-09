#!/bin/bash

# 상품 조회 성능 테스트 실행 스크립트

set -e

echo "🚀 상품 조회 성능 테스트 시작"
echo "================================================"
echo ""

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 시작 시간
START_TIME=$(date +%s)

echo -e "${BLUE}📋 테스트 환경:${NC}"
echo "  - Framework: Spring Boot + JPA + QueryDSL"
echo "  - Data: 100,000 products + 100 brands"
echo "  - Database: Testcontainers MySQL"
echo ""

# 테스트 선택
echo -e "${BLUE}📝 테스트 옵션:${NC}"
echo "  1. 전체 성능 테스트 (모든 항목)"
echo "  2. 기본 조회 성능만"
echo "  3. 페이징 성능만"
echo "  4. 필터링 성능만"
echo "  5. 정렬 성능만"
echo "  6. 동시 조회 시뮬레이션만"
echo ""

read -p "선택 (1-6): " choice

case $choice in
    1)
        echo -e "${YELLOW}⏳ 전체 성능 테스트 시작... (5-10분 소요)${NC}"
        ./gradlew test --tests ProductPerformanceTest -Dspring.profiles.active=test
        ;;
    2)
        echo -e "${YELLOW}⏳ 기본 조회 성능 테스트 시작...${NC}"
        ./gradlew test --tests "ProductPerformanceTest\$BasicQueryPerformance" -Dspring.profiles.active=test
        ;;
    3)
        echo -e "${YELLOW}⏳ 페이징 성능 테스트 시작...${NC}"
        ./gradlew test --tests "ProductPerformanceTest\$PagingPerformance" -Dspring.profiles.active=test
        ;;
    4)
        echo -e "${YELLOW}⏳ 필터링 성능 테스트 시작...${NC}"
        ./gradlew test --tests "ProductPerformanceTest\$FilteringPerformance" -Dspring.profiles.active=test
        ;;
    5)
        echo -e "${YELLOW}⏳ 정렬 성능 테스트 시작...${NC}"
        ./gradlew test --tests "ProductPerformanceTest\$SortingPerformance" -Dspring.profiles.active=test
        ;;
    6)
        echo -e "${YELLOW}⏳ 동시 조회 시뮬레이션 시작...${NC}"
        ./gradlew test --tests "ProductPerformanceTest\$ConcurrentQueryPerformance" -Dspring.profiles.active=test
        ;;
    *)
        echo -e "${RED}❌ 잘못된 선택${NC}"
        exit 1
        ;;
esac

# 완료 시간
END_TIME=$(date +%s)
ELAPSED=$((END_TIME - START_TIME))

echo ""
echo "================================================"
echo -e "${GREEN}✅ 테스트 완료!${NC}"
echo -e "소요 시간: ${ELAPSED}초"
echo ""
echo -e "${BLUE}📊 결과 분석:${NC}"
echo "  1. 콘솔 출력에서 성능 수치 확인"
echo "  2. 각 항목의 ⏱️ 표시 시간 vs 기준 비교"
echo "  3. 기준 미달 항목 → PERFORMANCE_TEST_GUIDE.md 참고"
echo ""
echo -e "${BLUE}📝 관련 파일:${NC}"
echo "  - PERFORMANCE_TEST_GUIDE.md (실행 가이드)"
echo "  - PERFORMANCE_TEST_REPORT.md (상세 리포트)"
echo "  - SAMPLE_DATA_GUIDE.md (샘플 데이터 가이드)"
