package com.loopers.application.user.like

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.like.ProductLikeRepository
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal

@DisplayName("UserProductLike UseCase 통합 테스트")
@SpringBootTest
class UserProductLikeUseCaseIntegrationTest
@Autowired
constructor(
    private val registerUseCase: UserProductLikeRegisterUseCase,
    private val cancelUseCase: UserProductLikeCancelUseCase,
    private val productRepository: ProductRepository,
    private val productLikeRepository: ProductLikeRepository,
    private val brandRepository: BrandRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ADMIN = "loopers.admin"
        private const val USER_ID = 1L
    }

    private var productId: Long = 0

    @BeforeEach
    fun setUp() {
        val brand = brandRepository.save(Brand.register(name = "테스트브랜드"), ADMIN)
        val activeBrand = brandRepository.save(brand.update("테스트브랜드", "ACTIVE"), ADMIN)

        val product = Product.register(
            name = "통합테스트 상품",
            regularPrice = Money(BigDecimal.valueOf(10000)),
            sellingPrice = Money(BigDecimal.valueOf(10000)),
            brandId = activeBrand.id!!,
        )
        val saved = productRepository.save(product, ADMIN)
        productId = productRepository.save(saved.activate(), ADMIN).id!!
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Nested
    @DisplayName("register 성공")
    inner class WhenRegisterSuccess {
        @Test
        @DisplayName("register 후 product_like row=1, likeCount=1이 된다")
        fun register_success_rowAndLikeCount() {
            registerUseCase.register(UserProductLikeCommand.Register(userId = USER_ID, productId = productId))

            val product = productRepository.findById(productId)!!
            assertAll(
                { assertThat(productLikeRepository.countByProductId(productId)).isEqualTo(1) },
                { assertThat(product.likeCount).isEqualTo(1) },
            )
        }
    }

    @Nested
    @DisplayName("cancel 성공")
    inner class WhenCancelSuccess {
        @Test
        @DisplayName("register 후 cancel 시 product_like row=0, likeCount=0이 된다")
        fun cancel_success_rowAndLikeCount() {
            registerUseCase.register(UserProductLikeCommand.Register(userId = USER_ID, productId = productId))
            cancelUseCase.cancel(UserProductLikeCommand.Cancel(userId = USER_ID, productId = productId))

            val product = productRepository.findById(productId)!!
            assertAll(
                { assertThat(productLikeRepository.countByProductId(productId)).isEqualTo(0) },
                { assertThat(product.likeCount).isEqualTo(0) },
            )
        }
    }

    @Nested
    @DisplayName("cancel no-op")
    inner class WhenCancelNoOp {
        @Test
        @DisplayName("like row 없을 때 cancel 시 row 변화 없음, likeCount 변화 없음")
        fun cancel_noOp_rowAndLikeCountUnchanged() {
            val likeCountBefore = productRepository.findById(productId)!!.likeCount

            cancelUseCase.cancel(UserProductLikeCommand.Cancel(userId = USER_ID, productId = productId))

            val product = productRepository.findById(productId)!!
            assertAll(
                { assertThat(productLikeRepository.countByProductId(productId)).isEqualTo(0) },
                { assertThat(product.likeCount).isEqualTo(likeCountBefore) },
            )
        }
    }
}
