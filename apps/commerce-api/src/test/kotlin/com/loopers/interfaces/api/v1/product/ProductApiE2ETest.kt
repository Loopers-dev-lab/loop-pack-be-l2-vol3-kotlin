package com.loopers.interfaces.api.v1.product

import com.loopers.testcontainers.MySqlTestContainersConfig
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.test.context.jdbc.Sql

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(MySqlTestContainersConfig::class)
@Sql(
    statements = ["DELETE FROM product_image", "DELETE FROM product", "DELETE FROM brand"],
    executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
)
class ProductApiE2ETest {

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
    }

    @Nested
    inner class GetProducts {
        @Test
        fun `상품 목록을 조회할 수 있다`() {
            val brandId = createBrand()
            createProduct(brandId)

            RestAssured.given()
            .`when`()
                .get("/api/v1/products")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("meta.result", equalTo("SUCCESS"))
                .body("data.size()", greaterThanOrEqualTo(1))
        }

        @Test
        fun `brandId로 필터링하여 조회할 수 있다`() {
            val brandId1 = createBrand("브랜드A")
            val brandId2 = createBrand("브랜드B")
            createProduct(brandId1, "상품A")
            createProduct(brandId2, "상품B")

            RestAssured.given()
                .queryParam("brandId", brandId1)
            .`when`()
                .get("/api/v1/products")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.size()", equalTo(1))
        }

        @Test
        fun `삭제된 상품은 목록에 노출되지 않는다`() {
            val brandId = createBrand()
            val productId = createProduct(brandId)
            deleteProduct(productId)

            RestAssured.given()
            .`when`()
                .get("/api/v1/products")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.size()", equalTo(0))
        }
    }

    @Nested
    inner class GetProduct {
        @Test
        fun `존재하는 상품을 정상 조회할 수 있다`() {
            val brandId = createBrand()
            val productId = createProduct(brandId)

            RestAssured.given()
            .`when`()
                .get("/api/v1/products/$productId")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("meta.result", equalTo("SUCCESS"))
                .body("data.id", equalTo(productId.toInt()))
        }

        @Test
        fun `존재하지 않는 상품을 조회하면 404를 반환한다`() {
            RestAssured.given()
            .`when`()
                .get("/api/v1/products/9999")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("NOT_FOUND"))
        }

        @Test
        fun `삭제된 상품을 조회하면 404를 반환한다`() {
            val brandId = createBrand()
            val productId = createProduct(brandId)
            deleteProduct(productId)

            RestAssured.given()
            .`when`()
                .get("/api/v1/products/$productId")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("NOT_FOUND"))
        }
    }

    private fun createBrand(name: String = BRAND_NAME): Long {
        return RestAssured.given()
            .contentType(ContentType.JSON)
            .body(mapOf("name" to name, "description" to null, "logoUrl" to null))
        .`when`()
            .post("/api-admin/v1/brands")
        .then()
            .statusCode(HttpStatus.CREATED.value())
            .extract()
            .jsonPath()
            .getLong("data.id")
    }

    private fun createProduct(brandId: Long, name: String = PRODUCT_NAME): Long {
        return RestAssured.given()
            .contentType(ContentType.JSON)
            .body(
                mapOf(
                    "brandId" to brandId,
                    "name" to name,
                    "description" to "설명",
                    "price" to 10000,
                    "stock" to 100,
                    "thumbnailUrl" to null,
                    "images" to emptyList<Any>(),
                ),
            )
        .`when`()
            .post("/api-admin/v1/products")
        .then()
            .statusCode(HttpStatus.CREATED.value())
            .extract()
            .jsonPath()
            .getLong("data.id")
    }

    private fun deleteProduct(productId: Long) {
        RestAssured.given()
        .`when`()
            .delete("/api-admin/v1/products/$productId")
        .then()
            .statusCode(HttpStatus.OK.value())
    }

    companion object {
        private const val BRAND_NAME = "테스트브랜드"
        private const val PRODUCT_NAME = "테스트상품"
    }
}
