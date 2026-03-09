import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

/**
 * 상품 데이터 10만개 생성 스크립트
 * - Brand: 100개 (다양한 브랜드)
 * - Product: 100,000개 이상 (다양한 분포)
 */

val random = Random(System.currentTimeMillis())
val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
val now = LocalDateTime.now().format(formatter)

// Brand 이름 리스트 (100개)
val brandNames = (1..100).map { idx ->
    when {
        idx <= 20 -> "프리미엄_브랜드_$idx"
        idx <= 40 -> "럭셔리_$idx"
        idx <= 60 -> "캐주얼_브랜드_$idx"
        idx <= 80 -> "스포츠_$idx"
        else -> "라이프스타일_$idx"
    }
}

// 상품명 템플릿 (다양한 카테고리)
val categories = listOf(
    "스마트폰", "노트북", "태블릿", "이어폰",
    "카메라", "시계", "신발", "가방",
    "의류", "액세서리", "가전제품", "게이밍기기",
    "헤드폰", "마우스", "키보드", "모니터",
    "프린터", "스캐너", "라우터", "보조배터리"
)

// ProductStatus 분포: ACTIVE 80%, INACTIVE 20%
fun getRandomStatus(): String {
    return if (random.nextInt(100) < 80) "ACTIVE" else "INACTIVE"
}

// 가격: 1,000 ~ 500,000 범위 (다양한 분포)
fun getRandomPrice(): String {
    val price = when (random.nextInt(10)) {
        in 0..2 -> random.nextInt(10000) + 1000          // 1,000 ~ 11,000 (저가 상품)
        in 3..6 -> random.nextInt(100000) + 50000        // 50,000 ~ 150,000 (일반 상품)
        else -> random.nextInt(400000) + 100000          // 100,000 ~ 500,000 (고가 상품)
    }
    return price.toString()
}

// 재고: 0 ~ 10,000 범위 (지수 분포)
fun getRandomStock(): Int {
    return when (random.nextInt(100)) {
        in 0..5 -> 0                                       // 5% - 재고 없음
        in 6..20 -> random.nextInt(100) + 1              // 15% - 1 ~ 100 (적음)
        in 21..60 -> random.nextInt(1000) + 100          // 40% - 100 ~ 1,100 (보통)
        else -> random.nextInt(9000) + 1000              // 40% - 1,000 ~ 10,000 (많음)
    }
}

// Like Count: 0 ~ 1,000 범위 (지수 분포)
fun getRandomLikeCount(): Int {
    return when (random.nextInt(100)) {
        in 0..40 -> 0                                     // 40% - 좋아요 없음
        in 41..70 -> random.nextInt(100)                 // 30% - 0 ~ 100
        in 71..90 -> random.nextInt(500) + 100           // 20% - 100 ~ 600
        else -> random.nextInt(400) + 600                // 10% - 600 ~ 1,000
    }
}

// SQL INSERT 스크립트 생성
fun generateSqlScript(): String {
    val output = StringBuilder()

    // 1. Brand 데이터 삽입 (100개)
    output.append("-- ========================================\n")
    output.append("-- Brands Data (100개)\n")
    output.append("-- ========================================\n")

    for ((idx, brandName) in brandNames.withIndex()) {
        val id = idx + 1
        val description = "브랜드 설명: $brandName - 프리미엄 품질과 다양한 제품 라인업을 자랑합니다"
        output.append("INSERT INTO brands (id, name, description, created_at, updated_at) ")
        output.append("VALUES ($id, '$brandName', '$description', '$now', '$now');\n")
    }

    output.append("\n-- ========================================\n")
    output.append("-- Products Data (100,000개)\n")
    output.append("-- ========================================\n")

    // 2. Product 데이터 삽입 (100,000개)
    var productId = 1
    val totalProducts = 100000

    for (i in 1..totalProducts) {
        val brandId = random.nextInt(100) + 1
        val category = categories[random.nextInt(categories.size)]
        val productName = "$category ${String.format("%06d", i)}"
        val price = getRandomPrice()
        val stock = getRandomStock()
        val status = getRandomStatus()
        val likeCount = getRandomLikeCount()

        output.append("INSERT INTO products (id, brand_id, name, price, stock, status, like_count, created_at, updated_at) ")
        output.append("VALUES ($productId, $brandId, '$productName', $price.00, $stock, '$status', $likeCount, '$now', '$now');\n")

        productId++

        // 진행률 표시 (콘솔)
        if (i % 10000 == 0) {
            System.err.println("Generated: $i/$totalProducts products")
        }
    }

    return output.toString()
}

// 메인 실행
fun main() {
    println("상품 데이터 생성 시작...")
    println("- Brand: ${brandNames.size}개")
    println("- Product: 100,000개")
    println()

    val sqlScript = generateSqlScript()

    // 파일에 저장
    val outputFile = File("/Users/chuljoongkim/Documents/loopers/loop-pack-be-l2-vol3-kotlin/sample-data-insert.sql")
    outputFile.writeText(sqlScript)

    println("SQL 스크립트 생성 완료!")
    println("파일 위치: ${outputFile.absolutePath}")
    println("파일 크기: ${outputFile.length() / 1024 / 1024}MB")
    println()
    println("사용 방법:")
    println("1. MySQL에 접속: mysql -u root -p loopers")
    println("2. SQL 스크립트 실행: source sample-data-insert.sql;")
}

main()
