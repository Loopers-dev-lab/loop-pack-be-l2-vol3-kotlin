package com.loopers.application.product

import com.loopers.application.brand.BrandInfo
import com.loopers.domain.product.Description
import com.loopers.domain.product.ImageUrl
import com.loopers.domain.product.Name
import com.loopers.domain.product.Price
import com.loopers.domain.product.ProductModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class ProductInfoTest {

    private fun setField(obj: Any, fieldName: String, value: Any) {
        var clazz: Class<*>? = obj.javaClass
        while (clazz != null) {
            try {
                val field = clazz.getDeclaredField(fieldName)
                field.isAccessible = true
                field.set(obj, value)
                return
            } catch (e: NoSuchFieldException) {
                clazz = clazz.superclass
            }
        }
    }

    @Test
    fun `ProductModel과 BrandInfo에서 ProductInfo로 변환 시 모든 필드가 올바르게 매핑된다`() {
        // given
        val now = ZonedDateTime.now()
        val productModel = ProductModel(
            brandId = 1L,
            name = Name("뉴발란스 991"),
            imageUrl = ImageUrl("test.png"),
            description = Description("편안한 신발"),
            price = Price(299_000L),
        )
        setField(productModel, "createdAt", now)
        setField(productModel, "updatedAt", now)

        val brandInfo = BrandInfo(
            id = 1L,
            name = "Nike",
            logoImageUrl = "logo.png",
            description = "Nike 브랜드",
            zipCode = "12345",
            roadAddress = "서울특별시 중구 명동길 14",
            detailAddress = "1층",
            email = "nike@google.com",
            phoneNumber = "02-3783-4401",
            businessNumber = "123-45-67890",
            createdAt = now,
            updatedAt = now,
        )

        // when
        val productInfo = ProductInfo.from(productModel, brandInfo)

        // then
        assertThat(productInfo.id).isEqualTo(productModel.id)
        assertThat(productInfo.brandId).isEqualTo(1L)
        assertThat(productInfo.name).isEqualTo("뉴발란스 991")
        assertThat(productInfo.imageUrl).isEqualTo("test.png")
        assertThat(productInfo.description).isEqualTo("편안한 신발")
        assertThat(productInfo.price).isEqualTo(299_000L)
        assertThat(productInfo.likeCount).isEqualTo(0L)
        assertThat(productInfo.brand).isEqualTo(brandInfo)
        assertThat(productInfo.createdAt).isEqualTo(now)
        assertThat(productInfo.updatedAt).isEqualTo(now)
    }

    @Test
    fun `likeCount가 0인 상품 변환 시 likeCount는 0이다`() {
        // given
        val now = ZonedDateTime.now()
        val productModel = ProductModel(
            brandId = 2L,
            name = Name("Air Max"),
            imageUrl = ImageUrl("air.png"),
            description = Description("나이키 에어맥스"),
            price = Price(150_000L),
        )
        setField(productModel, "createdAt", now)
        setField(productModel, "updatedAt", now)

        val brandInfo = BrandInfo(
            id = 2L,
            name = "Adidas",
            logoImageUrl = "adidas.png",
            description = "아디다스",
            zipCode = "12345",
            roadAddress = "서울특별시 강남구 테헤란로 1",
            detailAddress = "2층",
            email = "adidas@example.com",
            phoneNumber = "02-1234-5678",
            businessNumber = "234-56-78901",
            createdAt = now,
            updatedAt = now,
        )

        // when
        val productInfo = ProductInfo.from(productModel, brandInfo)

        // then
        assertThat(productInfo.likeCount).isEqualTo(0L)
        assertThat(productInfo.price).isEqualTo(150_000L)
        assertThat(productInfo.brand.name).isEqualTo("Adidas")
    }
}
