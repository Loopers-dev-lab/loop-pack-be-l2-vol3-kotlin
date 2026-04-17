package com.loopers.batch.infrastructure.catalog

/**
 * 특정 시점의 상품 메트릭 스냅샷 (Reader → Writer 사이의 타입 안전한 계약).
 *
 * Map<String, Long>를 쓰면 필드명 오타나 rename이 컴파일 단계에서 잡히지 않아
 * silent하게 0 값으로 처리되는 위험이 있어 명시 데이터 클래스로 표현한다.
 */
data class ProductMetricsSnapshot(
    val productId: Long,
    val viewCount: Long,
    val likeCount: Long,
    val orderCount: Long,
    val date: java.time.LocalDate,
)
