package com.loopers.domain.cache

/**
 * 캐시 무효화 어노테이션.
 *
 * 메서드 실행 성공 후 해당 캐시 키를 삭제.
 * 트랜잭션 활성 시 afterCommit에서 삭제 → 롤백 시 캐시 유지.
 *
 * - key 생략 시 메서드 파라미터를 자동 조합하여 캐시 키 생성
 * - key 지정 시 {paramName} 템플릿으로 치환
 * - Redis 장애 시 graceful degradation (삭제 실패해도 도메인 로직 진행)
 *
 * 사용 예:
 * ```
 * @CacheEvict(cacheName = CacheName.PRODUCT_INFO_NAME, key = "{productId}")
 * @Transactional
 * fun updateProduct(productId: Long, command: UpdateProductCommand): Product { ... }
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class CacheEvict(
    /** 캐시 이름 */
    val cacheName: String,
    /** 키 템플릿. 생략 시 메서드 파라미터 자동 조합. {paramName}은 메서드 파라미터로 치환됨 */
    val key: String = "",
)
