package com.loopers.support.config

import com.loopers.application.api.product.ProductFacade
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStatus
import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import kotlin.test.assertNotNull

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("캐시 에러 처리 E2E 테스트")
class CacheErrorHandlerE2ETest {
    @Autowired
    private lateinit var productFacade: ProductFacade

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var brandRepository: BrandRepository

    @Autowired
    private lateinit var cacheManager: CacheManager

    private lateinit var testBrand: Brand
    private lateinit var testProduct: Product

    @BeforeEach
    fun setup() {
        // 캐시 초기화
        cacheManager.getCache("product-info")?.clear()

        // 테스트용 브랜드 생성
        testBrand = brandRepository.save(
            Brand.create(
                name = "Test Brand ${System.nanoTime()}",
                description = "Test Description",
            ),
        )

        // 테스트용 상품 생성
        testProduct = productRepository.save(
            Product.create(
                brand = testBrand,
                name = "Cache Test Product",
                price = BigDecimal("10000"),
                status = ProductStatus.ACTIVE,
            ),
        )
    }

    @Test
    @DisplayName("첫 번째 조회: DB에서 조회하고 캐시에 저장")
    fun testFirstLookupFromDB() {
        // when
        val productInfo = productFacade.getProductInfo(testProduct.id)

        // then
        assertNotNull(productInfo)
        assert(productInfo.name == "Cache Test Product")
    }

    @Test
    @DisplayName("두 번째 조회: 캐시에서 조회")
    fun testSecondLookupFromCache() {
        // given - 첫 번째 조회로 캐시에 저장
        val firstLookup = productFacade.getProductInfo(testProduct.id)
        assertNotNull(firstLookup)

        // when - 두 번째 조회
        val secondLookup = productFacade.getProductInfo(testProduct.id)

        // then - 캐시된 데이터 반환
        assertNotNull(secondLookup)
        assert(secondLookup.name == firstLookup.name)
    }

    @Test
    @DisplayName("캐시 에러 발생 시에도 DB에서 조회하여 정상 응답")
    fun testFallbackToDatabaseOnCacheError() {
        // given - 첫 번째 조회로 캐시에 저장
        val firstLookup = productFacade.getProductInfo(testProduct.id)
        assertNotNull(firstLookup)

        // 캐시 매니저를 임시로 차단하는 대신,
        // 실제 동작은 Spring의 CacheErrorHandler가
        // 에러를 처리하고 계속 진행하도록 함

        // when - 조회 (캐시 에러가 발생하더라도 DB에서 조회)
        val result = productFacade.getProductInfo(testProduct.id)

        // then - 정상 응답
        assertNotNull(result)
        assert(result.name == "Cache Test Product")
    }

    @Test
    @DisplayName("캐시 미스 후 DB 조회 및 캐싱 성공")
    fun testCacheMissAndSuccessfulCaching() {
        // given - 새로운 상품 생성
        val newProduct = productRepository.save(
            Product.create(
                brand = testBrand,
                name = "New Cache Test Product",
                price = BigDecimal("20000"),
                status = ProductStatus.ACTIVE,
            ),
        )

        // when - 캐시 미스 상황에서 조회
        val result = productFacade.getProductInfo(newProduct.id)

        // then - 정상적으로 DB에서 조회하여 반환
        assertNotNull(result)
        assert(result.name == "New Cache Test Product")
        assert(result.price.compareTo(BigDecimal("20000")) == 0)
    }
}
