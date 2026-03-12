package com.loopers.domain.cache

/**
 * Cache-Aside 읽기 캐시 어노테이션.
 *
 * 캐시 히트 → 반환, 미스 → 메서드 실행 → 캐시 저장.
 * cacheName에 따라 CacheName enum의 CacheType이 자동 적용됨.
 *
 * - key 생략 시 메서드 파라미터를 자동 조합하여 캐시 키 생성
 * - key 지정 시 {paramName} 템플릿으로 치환
 * - Redis 장애 시 graceful degradation (DB 직접 조회)
 *
 * 사용 예:
 * ```
 * @Cached(cacheName = CacheName.PRODUCT_INFO_NAME)
 * fun getProductInfo(productId: Long): ProductInfo { ... }
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Cached(
    /** 캐시 이름. CacheName enum의 key와 매핑 */
    val cacheName: String,
    /** 키 템플릿. 생략 시 메서드 파라미터 자동 조합. {paramName}은 메서드 파라미터로 치환됨 */
    val key: String = "",
)
