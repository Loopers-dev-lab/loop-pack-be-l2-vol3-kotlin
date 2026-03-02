package com.loopers.domain.product

import com.loopers.domain.product.Description
import com.loopers.domain.product.ImageUrl
import com.loopers.domain.product.Name
import com.loopers.domain.product.Price
import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class ProductServiceTest {

    private val productRepository: ProductRepository = mockk()

    private lateinit var productService: ProductService

    @BeforeEach
    fun setUp() {
        productService = ProductService(productRepository)
    }

    @Nested
    inner class Create {

        @Test
        fun `상품 생성 성공`() {
            // given
            every { productRepository.save(any()) } answers { firstArg() }
            every { productRepository.existsBy(any(), any()) } returns false

            // when
            val result = productService.createProduct(
                brandId = 1L,
                name = "뉴발란스 991",
                imageUrl = "test.png",
                description = "뉴발란스 신발",
                price = 299_000L,
            )

            // then
            assertNotNull(result)
            verify(exactly = 1) { productRepository.save(any()) }
        }

        @Test
        fun `이미 존재하는 상품이면 예외 반환`() {
            // given
            every { productRepository.existsBy(any(), any()) } returns true

            // when
            val exception = assertThrows<CoreException> {
                productService.createProduct(
                    brandId = 1L,
                    name = "뉴발란스 991",
                    imageUrl = "test.png",
                    description = "뉴발란스 신발",
                    price = 299_000L,
                )
            }

            // then
            assertEquals(ErrorType.CONFLICT, exception.errorType)
            assertEquals("이미 존재하는 상품입니다.", exception.customMessage)
            verify(exactly = 0) { productRepository.save(any()) }
        }
    }

    @Nested
    inner class Read {

        @Test
        fun `brandId 없을 때 전체 상품 목록 반환`() {
            // given
            val pageable = PageRequest.of(0, 10)
            val products = listOf(
                ProductModel(1L, Name("뉴발란스 991"), ImageUrl("test1.png"), Description("신발"), Price(299_000L)),
                ProductModel(1L, Name("뉴발란스 992"), ImageUrl("test2.png"), Description("신발"), Price(299_000L)),
            )
            every { productRepository.findAll(pageable) } returns PageImpl(products, pageable, 2L)

            // when
            val result = productService.getProducts(null, pageable)

            // then
            assertEquals(2, result.content.size)
            verify(exactly = 1) { productRepository.findAll(pageable) }
            verify(exactly = 0) { productRepository.findAllByBrandId(any(), any()) }
        }

        @Test
        fun `brandId 있을 때 해당 브랜드 상품만 반환`() {
            // given
            val pageable = PageRequest.of(0, 10)
            val products = listOf(
                ProductModel(1L, Name("뉴발란스 991"), ImageUrl("test1.png"), Description("신발"), Price(299_000L)),
            )
            every { productRepository.findAllByBrandId(1L, pageable) } returns PageImpl(products, pageable, 1L)

            // when
            val result = productService.getProducts(1L, pageable)

            // then
            assertEquals(1, result.content.size)
            verify(exactly = 1) { productRepository.findAllByBrandId(1L, pageable) }
            verify(exactly = 0) { productRepository.findAll(any()) }
        }

        @Test
        fun `상품이 없을 때 빈 목록 반환`() {
            // given
            val pageable = PageRequest.of(0, 10)
            every { productRepository.findAll(pageable) } returns PageImpl(emptyList(), pageable, 0L)

            // when
            val result = productService.getProducts(null, pageable)

            // then
            assertEquals(0, result.totalElements)
            assertTrue(result.isEmpty)
        }

        @Test
        fun `id로 상품 단건 조회 성공`() {
            // given
            val product = ProductModel(1L, Name("뉴발란스 991"), ImageUrl("test.png"), Description("신발"), Price(299_000L))
            every { productRepository.findById(1L) } returns product

            // when
            val result = productService.getProductById(1L)

            // then
            assertNotNull(result)
            verify(exactly = 1) { productRepository.findById(1L) }
        }

        @Test
        fun `존재하지 않는 id로 조회하면 예외 반환`() {
            // given
            every { productRepository.findById(99L) } returns null

            // when
            val exception = assertThrows<CoreException> {
                productService.getProductById(99L)
            }

            // then
            assertEquals(ErrorType.NOT_FOUND, exception.errorType)
        }
    }

    @Nested
    inner class Update {

        @Test
        fun `상품 수정 성공`() {
            // given
            val product = ProductModel(1L, Name("뉴발란스 991"), ImageUrl("test.png"), Description("신발"), Price(299_000L))
            every { productRepository.findById(1L) } returns product

            // when
            val result = productService.updateProduct(
                id = 1L,
                name = "뉴발란스 992",
                imageUrl = "new.png",
                description = "새 신발",
                price = 350_000L,
            )

            // then
            assertEquals("뉴발란스 992", result.name.value)
            assertEquals("new.png", result.imageUrl.value)
            assertEquals("새 신발", result.description.value)
            assertEquals(350_000L, result.price.value)
        }

        @Test
        fun `존재하지 않는 상품 수정 시 예외 반환`() {
            // given
            every { productRepository.findById(99L) } returns null

            // when
            val exception = assertThrows<CoreException> {
                productService.updateProduct(
                    id = 99L,
                    name = "뉴발란스 992",
                    imageUrl = "new.png",
                    description = "새 신발",
                    price = 350_000L,
                )
            }

            // then
            assertEquals(ErrorType.NOT_FOUND, exception.errorType)
        }
    }

    @Nested
    inner class Delete {

        @Test
        fun `상품 soft delete 성공`() {
            // given
            val product = ProductModel(1L, Name("뉴발란스 991"), ImageUrl("test.png"), Description("신발"), Price(299_000L))
            every { productRepository.findById(1L) } returns product

            // when
            productService.deleteProduct(1L)

            // then
            assertNotNull(product.deletedAt)
        }

        @Test
        fun `존재하지 않는 상품 삭제 시 예외 반환`() {
            // given
            every { productRepository.findById(99L) } returns null

            // when
            val exception = assertThrows<CoreException> {
                productService.deleteProduct(99L)
            }

            // then
            assertEquals(ErrorType.NOT_FOUND, exception.errorType)
        }
    }
}
