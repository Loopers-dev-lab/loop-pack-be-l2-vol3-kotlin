package com.loopers.interfaces.api.v1.order

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
        "DELETE FROM likes",
        "DELETE FROM product_image",
        "DELETE FROM product",
        "DELETE FROM brand",
        "DELETE FROM users",
    ],
    executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
)
class OrderApiE2ETest {

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
    }

    @Nested
    inner class CreateOrder {
        @Test
        fun `정상적인 경우 주문이 생성된다`() {
            registerUser()
            val brandId = createBrand()
            val productId = createProduct(brandId)

            RestAssured.given()
                .contentType(ContentType.JSON)
                .header(AuthenticationFilter.HEADER_LOGIN_ID, LOGIN_ID)
                .header(AuthenticationFilter.HEADER_LOGIN_PW, PASSWORD)
                .body(createOrderRequest(productId))
            .`when`()
                .post("/api/v1/orders")
            .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("meta.result", equalTo("SUCCESS"))
                .body("data.id", notNullValue())
        }

        @Test
        fun `항목이 비어있으면 400 에러가 발생한다`() {
            registerUser()

            RestAssured.given()
                .contentType(ContentType.JSON)
                .header(AuthenticationFilter.HEADER_LOGIN_ID, LOGIN_ID)
                .header(AuthenticationFilter.HEADER_LOGIN_PW, PASSWORD)
                .body(mapOf("items" to emptyList<Any>()))
            .`when`()
                .post("/api/v1/orders")
            .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("BAD_REQUEST"))
        }

        @Test
        fun `재고가 부족하면 409 에러가 발생한다`() {
            registerUser()
            val brandId = createBrand()
            val productId = createProduct(brandId, stock = 1)

            RestAssured.given()
                .contentType(ContentType.JSON)
                .header(AuthenticationFilter.HEADER_LOGIN_ID, LOGIN_ID)
                .header(AuthenticationFilter.HEADER_LOGIN_PW, PASSWORD)
                .body(createOrderRequest(productId, quantity = 100))
            .`when`()
                .post("/api/v1/orders")
            .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("INSUFFICIENT_STOCK"))
        }

        @Test
        fun `인증 없이 주문하면 401 에러가 발생한다`() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body(createOrderRequest(1L))
            .`when`()
                .post("/api/v1/orders")
            .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("UNAUTHORIZED"))
        }
    }

    @Nested
    inner class GetOrders {
        @Test
        fun `주문 목록을 조회할 수 있다`() {
            registerUser()
            val brandId = createBrand()
            val productId = createProduct(brandId)
            createOrder(productId)

            RestAssured.given()
                .header(AuthenticationFilter.HEADER_LOGIN_ID, LOGIN_ID)
                .header(AuthenticationFilter.HEADER_LOGIN_PW, PASSWORD)
            .`when`()
                .get("/api/v1/orders")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("meta.result", equalTo("SUCCESS"))
                .body("data.size()", greaterThanOrEqualTo(1))
        }

        @Test
        fun `인증 없이 조회하면 401 에러가 발생한다`() {
            RestAssured.given()
            .`when`()
                .get("/api/v1/orders")
            .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("UNAUTHORIZED"))
        }
    }

    @Nested
    inner class GetOrder {
        @Test
        fun `주문 상세를 조회할 수 있다`() {
            registerUser()
            val brandId = createBrand()
            val productId = createProduct(brandId)
            val orderId = createOrder(productId)

            RestAssured.given()
                .header(AuthenticationFilter.HEADER_LOGIN_ID, LOGIN_ID)
                .header(AuthenticationFilter.HEADER_LOGIN_PW, PASSWORD)
            .`when`()
                .get("/api/v1/orders/$orderId")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("meta.result", equalTo("SUCCESS"))
                .body("data.id", equalTo(orderId.toInt()))
        }

        @Test
        fun `존재하지 않는 주문을 조회하면 404를 반환한다`() {
            registerUser()

            RestAssured.given()
                .header(AuthenticationFilter.HEADER_LOGIN_ID, LOGIN_ID)
                .header(AuthenticationFilter.HEADER_LOGIN_PW, PASSWORD)
            .`when`()
                .get("/api/v1/orders/9999")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("NOT_FOUND"))
        }

        @Test
        fun `인증 없이 조회하면 401 에러가 발생한다`() {
            RestAssured.given()
            .`when`()
                .get("/api/v1/orders/1")
            .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("UNAUTHORIZED"))
        }
    }

    @Nested
    inner class CancelOrder {
        @Test
        fun `PENDING 상태의 주문을 정상적으로 취소할 수 있다`() {
            registerUser()
            val brandId = createBrand()
            val productId = createProduct(brandId)
            val orderId = createOrder(productId)

            RestAssured.given()
                .header(AuthenticationFilter.HEADER_LOGIN_ID, LOGIN_ID)
                .header(AuthenticationFilter.HEADER_LOGIN_PW, PASSWORD)
            .`when`()
                .delete("/api/v1/orders/$orderId")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("meta.result", equalTo("SUCCESS"))
        }

        @Test
        fun `이미 취소된 주문을 다시 취소하면 400 에러가 발생한다`() {
            registerUser()
            val brandId = createBrand()
            val productId = createProduct(brandId)
            val orderId = createOrder(productId)
            cancelOrder(orderId)

            RestAssured.given()
                .header(AuthenticationFilter.HEADER_LOGIN_ID, LOGIN_ID)
                .header(AuthenticationFilter.HEADER_LOGIN_PW, PASSWORD)
            .`when`()
                .delete("/api/v1/orders/$orderId")
            .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("NOT_CANCELLABLE"))
        }

        @Test
        fun `존재하지 않는 주문을 취소하면 404를 반환한다`() {
            registerUser()

            RestAssured.given()
                .header(AuthenticationFilter.HEADER_LOGIN_ID, LOGIN_ID)
                .header(AuthenticationFilter.HEADER_LOGIN_PW, PASSWORD)
            .`when`()
                .delete("/api/v1/orders/9999")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("NOT_FOUND"))
        }

        @Test
        fun `인증 없이 취소하면 401 에러가 발생한다`() {
            RestAssured.given()
            .`when`()
                .delete("/api/v1/orders/1")
            .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("UNAUTHORIZED"))
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

    private fun createProduct(brandId: Long, stock: Int = 100): Long {
        return RestAssured.given()
            .contentType(ContentType.JSON)
            .body(
                mapOf(
                    "brandId" to brandId,
                    "name" to "테스트상품",
                    "description" to "설명",
                    "price" to 10000,
                    "stock" to stock,
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
            .body(createOrderRequest(productId))
        .`when`()
            .post("/api/v1/orders")
        .then()
            .statusCode(HttpStatus.CREATED.value())
            .extract()
            .jsonPath()
            .getLong("data.id")
    }

    private fun cancelOrder(orderId: Long) {
        RestAssured.given()
            .header(AuthenticationFilter.HEADER_LOGIN_ID, LOGIN_ID)
            .header(AuthenticationFilter.HEADER_LOGIN_PW, PASSWORD)
        .`when`()
            .delete("/api/v1/orders/$orderId")
        .then()
            .statusCode(HttpStatus.OK.value())
    }

    private fun createOrderRequest(productId: Long, quantity: Int = 2) = mapOf(
        "items" to listOf(mapOf("productId" to productId, "quantity" to quantity)),
    )

    companion object {
        private const val LOGIN_ID = "testuser"
        private const val PASSWORD = "Password1!"
    }
}
