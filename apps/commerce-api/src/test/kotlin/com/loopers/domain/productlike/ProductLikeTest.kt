package com.loopers.domain.productlike

import com.loopers.domain.brand.Brand
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductStatus
import com.loopers.domain.user.User
import com.loopers.domain.user.vo.BirthDate
import com.loopers.domain.user.vo.Email
import com.loopers.domain.user.vo.LoginId
import com.loopers.domain.user.vo.Name
import com.loopers.domain.user.vo.Password
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("ProductLike")
class ProductLikeTest {

    @DisplayName("좋아요 엔티티를 생성할 수 있다")
    @Test
    fun canCreateProductLike() {
        // arrange
        val brand = Brand.create(name = "Test Brand", description = "Test Description")
        val product = Product.create(
            brand = brand,
            name = "Test Product",
            price = BigDecimal("10000.00"),
            stock = 100,
            status = ProductStatus.ACTIVE,
        )
        val user = User.create(
            loginId = LoginId.of("testuser"),
            password = Password.ofEncrypted("encodedPassword"),
            name = Name.of("테스트 사용자"),
            birthDate = BirthDate.of("20000101"),
            email = Email.of("test@example.com"),
        )

        // act
        val productLike = ProductLike.create(user, product)

        // assert
        assertThat(productLike).isNotNull
        assertThat(productLike.user).isEqualTo(user)
        assertThat(productLike.product).isEqualTo(product)
        assertThat(productLike.userId).isEqualTo(user.id)
        assertThat(productLike.productId).isEqualTo(product.id)
    }
}
