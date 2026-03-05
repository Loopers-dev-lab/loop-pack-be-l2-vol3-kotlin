package com.loopers.application.admin.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.given
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import java.math.BigDecimal

@DisplayName("AdminProductUpdateUseCase")
class AdminProductUpdateUseCaseTest {
    private val productRepository: ProductRepository = mock()
    private val brandRepository: BrandRepository = mock()
    private val useCase = AdminProductUpdateUseCase(productRepository, brandRepository)

    private val admin = "loopers.admin"

    private fun inactiveProduct(id: Long = 1L, brandId: Long = 1L): Product = Product.retrieve(
        id = id,
        name = "기존 상품",
        regularPrice = Money(BigDecimal("10000")),
        sellingPrice = Money(BigDecimal("8000")),
        brandId = brandId,
        imageUrl = null,
        thumbnailUrl = null,
        likeCount = 0,
        status = Product.Status.INACTIVE,
    )

    private fun activeProduct(id: Long = 1L, brandId: Long = 1L): Product = Product.retrieve(
        id = id,
        name = "기존 상품",
        regularPrice = Money(BigDecimal("10000")),
        sellingPrice = Money(BigDecimal("8000")),
        brandId = brandId,
        imageUrl = null,
        thumbnailUrl = null,
        likeCount = 0,
        status = Product.Status.ACTIVE,
    )

    private fun command(
        productId: Long = 1L,
        status: String = "INACTIVE",
    ): AdminProductCommand.Update = AdminProductCommand.Update(
        productId = productId,
        name = "변경 상품",
        regularPrice = BigDecimal("12000"),
        sellingPrice = BigDecimal("9000"),
        status = status,
        imageUrl = "http://img.com/new.jpg",
        thumbnailUrl = null,
        admin = admin,
    )

    @Nested
    @DisplayName("상품이 존재하면 정보를 변경할 수 있다")
    inner class WhenProductExists {
        @Test
        @DisplayName("이름, 가격, 이미지가 변경된다")
        fun update_changeInfo() {
            val product = inactiveProduct()
            given(productRepository.findById(1L)).willReturn(product)
            given(productRepository.save(any(), eq(admin))).willAnswer { it.arguments[0] as Product }

            val result = useCase.update(command(status = "INACTIVE"))

            assertAll(
                { assertThat(result.name).isEqualTo("변경 상품") },
                { assertThat(result.regularPrice).isEqualByComparingTo(BigDecimal("12000")) },
                { assertThat(result.sellingPrice).isEqualByComparingTo(BigDecimal("9000")) },
                { assertThat(result.imageUrl).isEqualTo("http://img.com/new.jpg") },
            )
        }
    }

    @Nested
    @DisplayName("상품이 존재하지 않으면 수정에 실패한다")
    inner class WhenProductNotFound {
        @Test
        @DisplayName("PRODUCT_NOT_FOUND 예외를 던진다")
        fun update_notFound() {
            given(productRepository.findById(1L)).willReturn(null)

            val exception = assertThrows<CoreException> { useCase.update(command()) }
            assertThat(exception.errorType).isEqualTo(ErrorType.PRODUCT_NOT_FOUND)
        }
    }

    @Nested
    @DisplayName("INACTIVE → ACTIVE 상태 변경 시 브랜드가 ACTIVE이면 활성화에 성공한다")
    inner class WhenActivateWithActiveBrand {
        @Test
        @DisplayName("상품 상태가 ACTIVE로 변경된다")
        fun update_activate() {
            val product = inactiveProduct()
            val brand = Brand.retrieve(id = 1L, name = "브랜드", status = Brand.Status.ACTIVE)
            given(productRepository.findById(1L)).willReturn(product)
            given(brandRepository.findById(1L)).willReturn(brand)
            given(productRepository.save(any(), eq(admin))).willAnswer { it.arguments[0] as Product }

            val result = useCase.update(command(status = "ACTIVE"))

            assertThat(result.status).isEqualTo("ACTIVE")
        }
    }

    @Nested
    @DisplayName("INACTIVE → ACTIVE 상태 변경 시 브랜드가 INACTIVE이면 실패한다")
    inner class WhenActivateWithInactiveBrand {
        @Test
        @DisplayName("PRODUCT_INVALID_STATUS 예외를 던진다")
        fun update_activateWithInactiveBrand() {
            val product = inactiveProduct()
            val brand = Brand.retrieve(id = 1L, name = "브랜드", status = Brand.Status.INACTIVE)
            given(productRepository.findById(1L)).willReturn(product)
            given(brandRepository.findById(1L)).willReturn(brand)

            val exception = assertThrows<CoreException> { useCase.update(command(status = "ACTIVE")) }
            assertThat(exception.errorType).isEqualTo(ErrorType.PRODUCT_INVALID_STATUS)
        }
    }

    @Nested
    @DisplayName("ACTIVE → INACTIVE 상태 변경은 브랜드 검증 없이 성공한다")
    inner class WhenDeactivate {
        @Test
        @DisplayName("상품 상태가 INACTIVE로 변경된다")
        fun update_deactivate() {
            val product = activeProduct()
            given(productRepository.findById(1L)).willReturn(product)
            given(productRepository.save(any(), eq(admin))).willAnswer { it.arguments[0] as Product }

            val result = useCase.update(command(status = "INACTIVE"))

            assertThat(result.status).isEqualTo("INACTIVE")
        }
    }

    @Nested
    @DisplayName("상태가 동일하면 상태 변경 로직을 건너뛴다")
    inner class WhenSameStatus {
        @Test
        @DisplayName("INACTIVE → INACTIVE 요청 시 브랜드 검증 없이 성공한다")
        fun update_sameStatus() {
            val product = inactiveProduct()
            given(productRepository.findById(1L)).willReturn(product)
            given(productRepository.save(any(), eq(admin))).willAnswer { it.arguments[0] as Product }

            val result = useCase.update(command(status = "INACTIVE"))

            assertThat(result.status).isEqualTo("INACTIVE")
        }
    }
}
