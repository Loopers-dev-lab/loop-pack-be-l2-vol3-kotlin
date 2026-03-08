package com.loopers.interfaces.api.admin.v1.order

import com.loopers.interfaces.api.auth.AuthenticationFilter
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
    statements = [
        "DELETE FROM order_item",
        "DELETE FROM orders",
        "DELETE FROM user_coupon",
        "DELETE FROM coupon",
        "DELETE FROM likes",
        "DELETE FROM product_image",
        "DELETE FROM product",
        "DELETE FROM brand",
        "DELETE FROM users",
    ],
    executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
)
class AdminOrderApiE2ETest {

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
    }

    @Nested
    inner class GetAll {
        @Test
        fun `전체 주문 목록을 조회할 수 있다`() {
            registerUser()
            val brandId = createBrand()
            val productId = createProduct(brandId)
            createOrder(productId)

            RestAssured.given()
            .`when`()
                .get("/api-admin/v1/orders")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("meta.result", equalTo("SUCCESS"))
                .body("data.size()", greaterThanOrEqualTo(1))
        }
    }

    @Nested
    inner class GetById {
        @Test
        fun `주문 상세를 조회할 수 있다`() {
            registerUser()
            val brandId = createBrand()
            val productId = createProduct(brandId)
            val orderId = createOrder(productId)

            RestAssured.given()
            .`when`()
                .get("/api-admin/v1/orders/$orderId")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("meta.result", equalTo("SUCCESS"))
                .body("data.id", equalTo(orderId.toInt()))
                .body("data.userId", notNullValue())
        }

        @Test
        fun `존재하지 않는 주문을 조회하면 404를 반환한다`() {
            RestAssured.given()
            .`when`()
                .get("/api-admin/v1/orders/9999")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("NOT_FOUND"))
        }
    }

    private fun registerUser() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(
                mapOf(
                    "loginId" to LOGIN_ID,
                    "password" to PASSWORD,
                    "name" to "테스트",
                    "birthDate" to "1993-04-01",
                    "email" to "test@example.com",
                    "gender" to "MALE",
                ),
            )
        .`when`()
            .post("/api/v1/users")
        .then()
            .statusCode(HttpStatus.CREATED.value())
    }

    private fun createBrand(): Long {
        return RestAssured.given()
            .contentType(ContentType.JSON)
            .body(mapOf("name" to "테스트브랜드", "description" to null, "logoUrl" to null))
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
            .body(
                mapOf(
                    "brandId" to brandId,
                    "name" to "테스트상품",
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

    private fun createOrder(productId: Long): Long {
        return RestAssured.given()
            .contentType(ContentType.JSON)
            .header(AuthenticationFilter.HEADER_LOGIN_ID, LOGIN_ID)
            .header(AuthenticationFilter.HEADER_LOGIN_PW, PASSWORD)
            .body(mapOf("items" to listOf(mapOf("productId" to productId, "quantity" to 2))))
        .`when`()
            .post("/api/v1/orders")
        .then()
            .statusCode(HttpStatus.CREATED.value())
            .extract()
            .jsonPath()
            .getLong("data.id")
    }

    companion object {
        private const val LOGIN_ID = "testuser"
        private const val PASSWORD = "Password1!"
    }
}
