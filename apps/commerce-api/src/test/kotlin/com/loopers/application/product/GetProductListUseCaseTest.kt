package com.loopers.application.product

import com.loopers.application.brand.BrandCommand
import com.loopers.application.brand.RegisterBrandUseCase
import com.loopers.domain.product.ProductSortType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class GetProductListUseCaseTest @Autowired constructor(
    private val registerProductUseCase: RegisterProductUseCase,
    private val getProductListUseCase: GetProductListUseCase,
    private val deleteProductUseCase: DeleteProductUseCase,
    private val registerBrandUseCase: RegisterBrandUseCase,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun registerBrand(name: String = "나이키"): Long {
        return registerBrandUseCase.execute(BrandCommand.Register(name = name)).id
    }

    private fun registerProduct(
        brandId: Long,
        name: String = "테스트 상품",
        price: Long = 10000,
        stock: Int = 100,
    ): ProductInfo {
        return registerProductUseCase.execute(
            ProductCommand.Register(
                brandId = brandId,
                name = name,
                description = "상품 설명",
                price = price,
                stock = stock,
                imageUrl = "https://example.com/image.jpg",
            ),
        )
    }

    @DisplayName("상품 목록 조회")
    @Nested
    inner class Execute {

        @DisplayName("페이지네이션이 적용된 목록을 조회할 수 있다")
        @Test
        fun pagination() {
            val brandId = registerBrand()
            repeat(5) { i -> registerProduct(brandId, name = "상품$i") }

            val result = getProductListUseCase.execute(
                ProductCommand.Search(page = 0, size = 3),
            )

            assertThat(result.content).hasSize(3)
            assertThat(result.totalElements).isEqualTo(5)
            assertThat(result.totalPages).isEqualTo(2)
        }

        @DisplayName("브랜드 필터로 조회할 수 있다")
        @Test
        fun filterByBrandId() {
            val nike = registerBrand("나이키")
            val adidas = registerBrand("아디다스")
            registerProduct(nike, name = "나이키 신발")
            registerProduct(adidas, name = "아디다스 신발")

            val result = getProductListUseCase.execute(
                ProductCommand.Search(brandId = nike),
            )

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].brandName).isEqualTo("나이키")
        }

        @DisplayName("가격순으로 정렬할 수 있다")
        @Test
        fun sortByPrice() {
            val brandId = registerBrand()
            registerProduct(brandId, name = "비싼 상품", price = 50000)
            registerProduct(brandId, name = "싼 상품", price = 10000)
            registerProduct(brandId, name = "중간 상품", price = 30000)

            val result = getProductListUseCase.execute(
                ProductCommand.Search(sort = ProductSortType.PRICE_ASC),
            )

            assertThat(result.content.map { it.price }).containsExactly(10000, 30000, 50000)
        }

        @DisplayName("삭제된 상품은 기본 조회에 포함되지 않는다")
        @Test
        fun excludeDeletedByDefault() {
            val brandId = registerBrand()
            registerProduct(brandId, name = "활성 상품")
            val deleted = registerProduct(brandId, name = "삭제 상품")
            deleteProductUseCase.execute(deleted.id)

            val result = getProductListUseCase.execute(ProductCommand.Search())

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].name).isEqualTo("활성 상품")
        }

        @DisplayName("brandName이 포함된다")
        @Test
        fun includesBrandName() {
            val brandId = registerBrand("나이키")
            registerProduct(brandId, name = "나이키 에어맥스")

            val result = getProductListUseCase.execute(ProductCommand.Search())

            assertThat(result.content[0].brandName).isEqualTo("나이키")
        }
    }
}
