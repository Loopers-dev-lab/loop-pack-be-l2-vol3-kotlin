package com.loopers.domain.product

import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.infrastructure.product.ProductRepositoryImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import kotlin.system.measureTimeMillis

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = ["/insert-data.sql"], executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Sql(scripts = ["/delete-data.sql"], executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
@DisplayName("상품 조회 성능 테스트 (100,000개 데이터)")
class ProductPerformanceTest @Autowired constructor(
    private val productJpaRepository: ProductJpaRepository,
    private val productRepository: ProductRepositoryImpl,
    private val jdbcTemplate: JdbcTemplate,
) {
    companion object {
        private const val EXPECTED_PRODUCT_COUNT = 100_000L
    }

    @DisplayName("기본 조회 성능")
    @Nested
    inner class BasicQueryPerformance {

        @DisplayName("전체 상품 개수 조회 - COUNT 성능")
        @Test
        fun countAllProducts_performance() {
            val duration = measureTimeMillis {
                val totalCount = productJpaRepository.count()
                assertThat(totalCount).isGreaterThanOrEqualTo(EXPECTED_PRODUCT_COUNT)
            }

            println("⏱️  전체 상품 개수 조회: ${duration}ms")
            assertThat(duration).isLessThan(2000)
        }

        @DisplayName("WHERE 절 쿼리 - 인덱스 vs Full Scan 비교")
        @Test
        fun countComparison_withIndexVsFullScan() {
            // 인덱스 사용
            val indexDuration = measureTimeMillis {
                val count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products FORCE INDEX (idx_status) WHERE status = 'ACTIVE'", Long::class.java)
                assertThat(count).isGreaterThan(0)
            }

            // Full Scan (인덱스 무시)
            val fullScanDuration = measureTimeMillis {
                val count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products IGNORE INDEX (idx_status) WHERE status = 'ACTIVE'", Long::class.java)
                assertThat(count).isGreaterThan(0)
            }

            // 결과 출력
            println("\n📊 WHERE 절 쿼리 성능 비교 (status = 'ACTIVE'):")
            println("✅ WITH INDEX (idx_status): ${indexDuration}ms")
            println("✅ FULL SCAN: ${fullScanDuration}ms")
        }

        @DisplayName("첫 페이지 상품 조회 (Page 1, Size 20)")
        @Test
        fun findFirstPage_performance() {
            val pageable = PageRequest.of(0, 20)

            val duration = measureTimeMillis {
                val products = productRepository.findWithPaging(null, pageable)
                assertThat(products.content).hasSize(20)
                assertThat(products.totalElements).isGreaterThanOrEqualTo(EXPECTED_PRODUCT_COUNT)
            }

            println("⏱️  첫 페이지 조회: ${duration}ms")
            println("   조회된 상품 수: 20개")
            assertThat(duration).isLessThan(500)
        }

        @DisplayName("단건 조회 성능 (ID로 검색)")
        @Test
        fun findById_performance() {
            // Warm-up: 첫 조회는 캐시 미스로 느릴 수 있음
            productRepository.findById(1L)

            val duration = measureTimeMillis {
                val product = productRepository.findById(1L)
                assertThat(product).isNotNull()
            }

            println("⏱️  단건 조회 (ID=1): ${duration}ms")
            assertThat(duration).isLessThan(150)
        }
    }

    @DisplayName("페이징 성능")
    @Nested
    inner class PagingPerformance {

        @DisplayName("중간 페이지 조회 (Page 50, Size 20) - OFFSET 성능")
        @Test
        fun findMiddlePage_performance() {
            val pageable = PageRequest.of(50, 20)

            val duration = measureTimeMillis {
                val products = productRepository.findWithPaging(null, pageable)
                assertThat(products.content).hasSize(20)
            }

            println("⏱️  중간 페이지 조회 (Page 50): ${duration}ms")
            assertThat(duration).isLessThan(800)
        }

        @DisplayName("뒷 페이지 조회 (Page 2,500, Size 20) - 큰 OFFSET 성능")
        @Test
        fun findLastPage_performance() {
            val pageable = PageRequest.of(2500, 20)

            val duration = measureTimeMillis {
                val products = productRepository.findWithPaging(null, pageable)
                assertThat(products.content).hasSizeGreaterThan(0)
            }

            println("⏱️  뒷 페이지 조회 (Page 2,500): ${duration}ms")
            assertThat(duration).isLessThan(1500)
        }

        @DisplayName("대량 페이징 조회 (Size 100) - 큰 페이지 크기")
        @Test
        fun findWithLargePageSize_performance() {
            val pageable = PageRequest.of(0, 100)

            val duration = measureTimeMillis {
                val products = productRepository.findWithPaging(null, pageable)
                assertThat(products.content).hasSize(100)
            }

            println("⏱️  대량 조회 (Page Size=100): ${duration}ms")
            assertThat(duration).isLessThan(800)
        }

        @DisplayName("연속 페이징 성능 - 전체 페이지 순회")
        @Test
        fun sequentialPaging_performance() {
            var totalDuration = 0L
            var totalRecords = 0
            val pageSize = 50
            val pageCount = 10

            for (page in 0 until pageCount) {
                val pageable = PageRequest.of(page, pageSize)
                val duration = measureTimeMillis {
                    val products = productRepository.findWithPaging(null, pageable)
                    totalRecords += products.content.size
                }
                totalDuration += duration
                println("  Page $page: ${duration}ms")
            }

            println("⏱️  연속 페이징 ($pageCount 페이지): ${totalDuration}ms (평균: ${totalDuration / pageCount}ms/page)")
            println("   총 조회 레코드: ${totalRecords}개")
            assertThat(totalDuration).isLessThan(10000)
        }
    }

    @DisplayName("필터링 성능")
    @Nested
    inner class FilteringPerformance {

        @DisplayName("활성 상품만 조회 (Status 필터링)")
        @Test
        fun findActiveProducts_performance() {
            val pageable = PageRequest.of(0, 50)

            val duration = measureTimeMillis {
                val products = productRepository.findActiveProductsWithPaging(null, pageable)
                assertThat(products.totalElements).isGreaterThan(70000)
            }

            println("⏱️  활성 상품 조회: ${duration}ms")
            println("   조회된 활성 상품: ~80,000개")
            assertThat(duration).isLessThan(800)
        }

        @DisplayName("특정 브랜드 상품 조회 (Brand ID 필터링)")
        @Test
        fun findByBrandId_performance() {
            val brandId = 5L
            val pageable = PageRequest.of(0, 50)

            val duration = measureTimeMillis {
                val products = productRepository.findWithPaging(brandId, pageable)
                println("   Brand $brandId total products: ${products.totalElements}")
                assertThat(products.totalElements).isGreaterThan(0)
            }

            println("⏱️  Brand filter query: ${duration}ms")
            assertThat(duration).isLessThan(500)
        }

        @DisplayName("활성 상품만 조회 (정렬 포함)")
        @Test
        fun findActiveWithSort_performance() {
            val pageable = PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "price"))

            val duration = measureTimeMillis {
                val products = productRepository.findActiveProductsWithPaging(null, pageable)
                println("   Active products: ${products.totalElements}")
                assertThat(products.totalElements).isGreaterThan(0)
            }

            println("⏱️  Active products with sort: ${duration}ms")
            assertThat(duration).isLessThan(600)
        }
    }

    @DisplayName("정렬 성능")
    @Nested
    inner class SortingPerformance {

        @DisplayName("가격순 정렬 (오름차순) - ORDER BY 성능")
        @Test
        fun sortByPrice_ascending_performance() {
            val pageable = PageRequest.of(
                0,
                50,
                Sort.by(Sort.Direction.ASC, "price"),
            )

            val duration = measureTimeMillis {
                val products = productRepository.findWithPaging(null, pageable)
                assertThat(products.content).isNotEmpty()
                println("   첫 상품: ${products.content.first().name} (가격: ${products.content.first().price})")
                println("   마지막 상품: ${products.content.last().name} (가격: ${products.content.last().price})")
            }

            println("⏱️  가격순 정렬 (오름): ${duration}ms")
            assertThat(duration).isLessThan(1000)
        }

        @DisplayName("가격순 정렬 (내림차순)")
        @Test
        fun sortByPrice_descending_performance() {
            val pageable = PageRequest.of(
                0,
                50,
                Sort.by(Sort.Direction.DESC, "price"),
            )

            val duration = measureTimeMillis {
                val products = productRepository.findWithPaging(null, pageable)
                assertThat(products.content).isNotEmpty()
                println("   첫 상품: ${products.content.first().name} (가격: ${products.content.first().price})")
            }

            println("⏱️  가격순 정렬 (내림): ${duration}ms")
            assertThat(duration).isLessThan(1000)
        }

        @DisplayName("생성일순 정렬 - 기본 정렬")
        @Test
        fun sortByCreatedAt_performance() {
            val pageable = PageRequest.of(
                0,
                50,
                Sort.by(Sort.Direction.DESC, "createdAt"),
            )

            val duration = measureTimeMillis {
                val products = productRepository.findWithPaging(null, pageable)
                assertThat(products.content).isNotEmpty()
            }

            println("⏱️  생성일순 정렬: ${duration}ms")
            assertThat(duration).isLessThan(800)
        }
    }

    @DisplayName("대량 동시 조회 시뮬레이션")
    @Nested
    inner class ConcurrentQueryPerformance {

        @DisplayName("순차 조회 성능 (10회 반복)")
        @Test
        fun sequential_queries_performance() {
            val results = mutableListOf<Long>()

            for (i in 1..10) {
                val duration = measureTimeMillis {
                    val products = productRepository.findWithPaging(
                        null,
                        PageRequest.of(i % 100, 20),
                    )
                    assertThat(products.content).isNotEmpty()
                }
                results.add(duration)
            }

            val avgDuration = results.average()
            val maxDuration = results.maxOrNull() ?: 0L

            println("⏱️  Sequential queries (10 times):")
            println("   Average: ${avgDuration.toLong()}ms")
            println("   Max: ${maxDuration}ms")
            println("   Details: ${results.mapIndexed { idx, ms -> "Page $idx: ${ms}ms" }.joinToString(", ")}")

            assertThat(avgDuration.toLong()).isLessThan(500)
            assertThat(maxDuration).isLessThan(1000)
        }

        @DisplayName("다양한 쿼리 패턴 조회 (복합 시나리오)")
        @Test
        fun mixed_query_patterns_performance() {
            val pageable1 = PageRequest.of(0, 20)
            val pageable2 = PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "price"))
            val pageable3 = PageRequest.of(100, 20)

            val duration = measureTimeMillis {
                // 1. 기본 조회
                val result1 = productRepository.findWithPaging(null, pageable1)
                assertThat(result1.content).hasSize(20)

                // 2. 정렬 조회 (전체 ACTIVE 상품)
                val result2 = productRepository.findActiveProductsWithPaging(null, pageable2)
                assertThat(result2.content).isNotEmpty()

                // 3. 깊은 페이징
                val result3 = productRepository.findWithPaging(null, pageable3)
                assertThat(result3.content).isNotEmpty()
            }

            println("⏱️  복합 쿼리 패턴 (3가지 패턴): ${duration}ms")
            assertThat(duration).isLessThan(2000)
        }
    }
}
