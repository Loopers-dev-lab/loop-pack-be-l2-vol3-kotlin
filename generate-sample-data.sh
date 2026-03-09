#!/bin/bash

# 상품 데이터 10만개 생성 스크립트
# - Brand: 100개 (다양한 브랜드)
# - Product: 100,000개 이상 (다양한 분포)

OUTPUT_FILE="sample-data-insert.sql"
NOW=$(date '+%Y-%m-%d %H:%M:%S')

echo "상품 데이터 생성 시작..."
echo "- Brand: 100개"
echo "- Product: 100,000개"
echo

{
    echo "-- ========================================"
    echo "-- Brands Data (100개)"
    echo "-- ========================================";
    echo ""

    # Brand 데이터 생성 (100개)
    for i in {1..100}; do
        case $((i)) in
            [1-9]|1[0-9]|2[0-9])
                BRAND_NAME="프리미엄_브랜드_$i"
                ;;
            [3-3][0-9]|40)
                BRAND_NAME="럭셔리_$(($i - 20))"
                ;;
            [4-5][0-9]|60)
                BRAND_NAME="캐주얼_브랜드_$(($i - 40))"
                ;;
            [6-7][0-9]|80)
                BRAND_NAME="스포츠_$(($i - 60))"
                ;;
            *)
                BRAND_NAME="라이프스타일_$(($i - 80))"
                ;;
        esac

        DESCRIPTION="브랜드 설명: $BRAND_NAME - 프리미엄 품질과 다양한 제품 라인업을 자랑합니다"
        echo "INSERT INTO brands (id, name, description, created_at, updated_at) VALUES ($i, '$BRAND_NAME', '$DESCRIPTION', '$NOW', '$NOW');"
    done

    echo ""
    echo "-- ========================================"
    echo "-- Products Data (100,000개)"
    echo "-- ========================================";
    echo ""

    # Product 데이터 생성 (100,000개)
    CATEGORIES=("스마트폰" "노트북" "태블릿" "이어폰" "카메라" "시계" "신발" "가방" "의류" "액세서리" "가전제품" "게이밍기기" "헤드폰" "마우스" "키보드" "모니터" "프린터" "스캐너" "라우터" "보조배터리")
    CATEGORY_COUNT=${#CATEGORIES[@]}

    for i in {1..100000}; do
        # Brand ID: 1~100 랜덤
        BRAND_ID=$((1 + RANDOM % 100))

        # 상품명: 카테고리 + 번호
        CATEGORY_IDX=$((RANDOM % CATEGORY_COUNT))
        CATEGORY="${CATEGORIES[$CATEGORY_IDX]}"
        PRODUCT_NAME="$CATEGORY $(printf "%06d" $i)"

        # 가격: 1,000 ~ 500,000 (다양한 분포)
        PRICE_RAND=$((RANDOM % 10))
        case $PRICE_RAND in
            [0-2])
                PRICE=$((1000 + RANDOM % 10000))
                ;;
            [3-6])
                PRICE=$((50000 + RANDOM % 100000))
                ;;
            *)
                PRICE=$((100000 + RANDOM % 400000))
                ;;
        esac

        # 재고: 0 ~ 10,000 (지수 분포)
        STOCK_RAND=$((RANDOM % 100))
        case $STOCK_RAND in
            [0-5])
                STOCK=0
                ;;
            [6-2][0])
                STOCK=$((1 + RANDOM % 100))
                ;;
            [2-6][0-9])
                STOCK=$((100 + RANDOM % 1000))
                ;;
            *)
                STOCK=$((1000 + RANDOM % 9000))
                ;;
        esac

        # 상태: ACTIVE 80%, INACTIVE 20%
        STATUS_RAND=$((RANDOM % 100))
        if [ $STATUS_RAND -lt 80 ]; then
            STATUS="ACTIVE"
        else
            STATUS="INACTIVE"
        fi

        # Like Count: 0 ~ 1,000 (지수 분포)
        LIKE_RAND=$((RANDOM % 100))
        case $LIKE_RAND in
            [0-3][0-9])
                LIKE_COUNT=0
                ;;
            [4-6][0-9])
                LIKE_COUNT=$((RANDOM % 100))
                ;;
            [7-8][0-9])
                LIKE_COUNT=$((100 + RANDOM % 500))
                ;;
            *)
                LIKE_COUNT=$((600 + RANDOM % 400))
                ;;
        esac

        echo "INSERT INTO products (id, brand_id, name, price, stock, status, like_count, created_at, updated_at) VALUES ($i, $BRAND_ID, '$PRODUCT_NAME', $PRICE.00, $STOCK, '$STATUS', $LIKE_COUNT, '$NOW', '$NOW');"

        # 진행률 표시 (10,000개마다)
        if [ $((i % 10000)) -eq 0 ]; then
            echo "-- Generated: $i/100000 products" >&2
        fi
    done

} > "$OUTPUT_FILE"

echo "✅ SQL 스크립트 생성 완료!"
echo "파일 위치: $(pwd)/$OUTPUT_FILE"
echo "파일 크기: $(du -h $OUTPUT_FILE | cut -f1)"
echo ""
echo "📝 사용 방법:"
echo "1. MySQL에 접속: mysql -u root -p loopers"
echo "2. SQL 스크립트 실행: source sample-data-insert.sql;"
