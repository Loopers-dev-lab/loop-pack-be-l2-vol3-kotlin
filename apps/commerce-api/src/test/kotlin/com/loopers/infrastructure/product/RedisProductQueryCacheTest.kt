package com.loopers.infrastructure.product

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.domain.product.ProductQueryResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.math.BigDecimal
import java.time.Duration

@ExtendWith(OutputCaptureExtension::class)
@DisplayName("RedisProductQueryCache")
class RedisProductQueryCacheTest {
    private val redisTemplate: RedisTemplate<String, String> = mock()
    private val valueOperations: ValueOperations<String, String> = mock()
    private val objectMapper: ObjectMapper = mock()
    private val cache = RedisProductQueryCache(redisTemplate, objectMapper)

    @BeforeEach
    fun setUp() {
        whenever(redisTemplate.opsForValue()).thenReturn(valueOperations)
        given(objectMapper.writeValueAsString(any())).willReturn("{}")
    }

    @Test
    @DisplayName("detail evict 실패 시 warning log 를 남기고 예외를 전파하지 않는다")
    fun evictDetails_logsWarningOnFailure(output: CapturedOutput) {
        given(redisTemplate.delete(any<Set<String>>())).willThrow(RuntimeException("boom"))

        assertDoesNotThrow {
            cache.evictDetails(listOf(1L, 2L))
        }

        assertThat(output.out)
            .contains("Failed to evict product detail cache")
            .contains("product:detail:v1:1")
            .contains("product:detail:v1:2")
    }

    @Test
    @DisplayName("list namespace invalidation 실패 시 warning log 를 남기고 예외를 전파하지 않는다")
    fun invalidateListsByBrandId_logsWarningOnFailure(output: CapturedOutput) {
        given(valueOperations.increment(eq("product:list:namespace:v1:brand:1"))).willThrow(RuntimeException("boom"))

        assertDoesNotThrow {
            cache.invalidateListsByBrandId(1L)
        }

        assertThat(output.out)
            .contains("Failed to invalidate product list namespace")
            .contains("brandId=1")
            .contains("product:list:namespace:v1:brand:1")
            .contains("product:list:namespace:v1:brand:all")
    }

    @Test
    @DisplayName("cache write 실패 시 warning log 를 남기고 예외를 전파하지 않는다")
    fun putDetail_logsWarningOnFailure(output: CapturedOutput) {
        doThrow(RuntimeException("boom"))
            .whenever(valueOperations)
            .set(eq("product:detail:v1:1"), eq("{}"), eq(Duration.ofMinutes(10)))

        assertDoesNotThrow {
            cache.putDetail(
                ProductQueryResult.Detail(
                    id = 1L,
                    name = "상품",
                    regularPrice = BigDecimal("10000"),
                    sellingPrice = BigDecimal("9000"),
                    brandId = 1L,
                    brandName = "브랜드",
                    imageUrl = null,
                    thumbnailUrl = null,
                    likeCount = 3,
                    stockQuantity = 10,
                ),
            )
        }

        assertThat(output.out)
            .contains("Failed to write product query cache")
            .contains("key=product:detail:v1:1")
    }
}
