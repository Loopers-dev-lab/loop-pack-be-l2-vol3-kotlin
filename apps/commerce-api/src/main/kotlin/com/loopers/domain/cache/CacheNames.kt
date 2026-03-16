package com.loopers.domain.cache

/**
 * 캐시 이름 상수.
 *
 * @Cached / @CacheEvict 어노테이션에서 참조하는 compile-time constant.
 * Domain 레이어에 위치하여 DIP를 준수한다.
 *
 * 실제 TTL, CacheType 등 인프라 설정은 infrastructure/cache/CacheName enum에서 관리.
 */
object CacheNames {
    const val PRODUCT_INFO = "product-info"
    const val BRAND_INFO = "brand-info"
    const val PRODUCT_LIST = "product-list"
}
