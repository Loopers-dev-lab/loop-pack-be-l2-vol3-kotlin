package com.loopers.interfaces.api.admin.v1.product

import com.loopers.testcontainers.MySqlTestContainersConfig
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.hamcrest.Matchers.notNullValue
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
class AdminProductApiE2ETest {

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
    }

    @Nested
    inner class Create {
        @Test
        fun `정상적인 경우 상품이 생성된다`() {
            val brandId = createBrand()

            RestAssured.given()
                .contentType(ContentType.JSON)
                .body(createProductRequest(brandId))
            .`when`()
                .post("/api-admin/v1/products")
            .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("meta.result", equalTo("SUCCESS"))
                .body("data.id", notNullValue())
        }

        @Test
        fun `이름이 빈값이면 400 에러가 발생한다`() {
            val brandId = createBrand()

            RestAssured.given()
                .contentType(ContentType.JSON)
                .body(
                    mapOf(
                        "brandId" to brandId,
                        "name" to "",
                        "price" to 10000,
                        "stock" to 100,
                    ),
                )
            .`when`()
                .post("/api-admin/v1/products")
            .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("BAD_REQUEST"))
        }

        @Test
        fun `브랜드가 존재하지 않으면 404 에러가 발생한다`() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body(createProductRequest(9999L))
            .`when`()
                .post("/api-admin/v1/products")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("NOT_FOUND"))
        }
    }

    @Nested
    inner class GetAll {
        @Test
        fun `상품 목록을 조회할 수 있다`() {
            val brandId = createBrand()
            createProduct(brandId)

            RestAssured.given()
            .`when`()
                .get("/api-admin/v1/products")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("meta.result", equalTo("SUCCESS"))
                .body("data.size()", greaterThanOrEqualTo(1))
        }
    }

    @Nested
    inner class GetById {
        @Test
        fun `존재하는 상품을 조회할 수 있다`() {
            val brandId = createBrand()
            val productId = createProduct(brandId)

            RestAssured.given()
            .`when`()
                .get("/api-admin/v1/products/$productId")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("meta.result", equalTo("SUCCESS"))
                .body("data.id", equalTo(productId.toInt()))
        }

        @Test
        fun `삭제된 상품도 조회할 수 있다`() {
            val brandId = createBrand()
            val productId = createProduct(brandId)
            deleteProduct(productId)

            RestAssured.given()
            .`when`()
                .get("/api-admin/v1/products/$productId")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("meta.result", equalTo("SUCCESS"))
                .body("data.deletedAt", notNullValue())
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `정상적인 경우 상품이 수정된다`() {
            val brandId = createBrand()
            val productId = createProduct(brandId)

            RestAssured.given()
                .contentType(ContentType.JSON)
                .body(
                    mapOf(
                        "name" to "수정된상품",
                        "description" to "수정됨",
                        "price" to 20000,
                        "stock" to 50,
                        "status" to "ACTIVE",
                        "images" to emptyList<Any>(),
                    ),
                )
            .`when`()
                .put("/api-admin/v1/products/$productId")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("meta.result", equalTo("SUCCESS"))
                .body("data.name", equalTo("수정된상품"))
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `정상적인 경우 상품이 삭제된다`() {
            val brandId = createBrand()
            val productId = createProduct(brandId)

            RestAssured.given()
            .`when`()
                .delete("/api-admin/v1/products/$productId")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("meta.result", equalTo("SUCCESS"))
        }
    }

    private fun createBrand(): Long {
        return RestAssured.given()
            .contentType(ContentType.JSON)
            .body(mapOf("name" to BRAND_NAME, "description" to null, "logoUrl" to null))
        .`when`()
            .post("/api-admin/v1/brands")
        .then()
            .statusCode(HttpStatus.CREATED.value())
            .extract()
            .jsonPath()
            .getLong("data.id")
    }

    private fun createProduct(brandId: Long): Long {
        return RestAssured.given()
            .contentType(ContentType.JSON)
            .body(createProductRequest(brandId))
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

    private fun createProductRequest(brandId: Long) = mapOf(
        "brandId" to brandId,
        "name" to PRODUCT_NAME,
        "description" to "설명",
        "price" to 10000,
        "stock" to 100,
        "thumbnailUrl" to null,
        "images" to emptyList<Any>(),
    )

    companion object {
        private const val BRAND_NAME = "테스트브랜드"
        private const val PRODUCT_NAME = "테스트상품"
    }
}
