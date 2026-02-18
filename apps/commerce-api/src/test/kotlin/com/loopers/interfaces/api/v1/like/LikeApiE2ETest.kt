package com.loopers.interfaces.api.v1.like

import com.loopers.interfaces.api.auth.AuthenticationFilter
import com.loopers.testcontainers.MySqlTestContainersConfig
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
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
        "DELETE FROM likes",
        "DELETE FROM product_image",
        "DELETE FROM product",
        "DELETE FROM brand",
        "DELETE FROM users",
    ],
    executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
)
class LikeApiE2ETest {

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
    }

    @Nested
    inner class AddLike {
        @Test
        fun `정상적인 경우 좋아요가 추가된다`() {
            registerUser()
            val brandId = createBrand()
            val productId = createProduct(brandId)

            RestAssured.given()
                .header(AuthenticationFilter.HEADER_LOGIN_ID, LOGIN_ID)
                .header(AuthenticationFilter.HEADER_LOGIN_PW, PASSWORD)
            .`when`()
                .post("/api/v1/products/$productId/likes")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("meta.result", equalTo("SUCCESS"))
        }

        @Test
        fun `중복 좋아요도 멱등하게 성공한다`() {
            registerUser()
            val brandId = createBrand()
            val productId = createProduct(brandId)

            RestAssured.given()
                .header(AuthenticationFilter.HEADER_LOGIN_ID, LOGIN_ID)
                .header(AuthenticationFilter.HEADER_LOGIN_PW, PASSWORD)
            .`when`()
                .post("/api/v1/products/$productId/likes")
            .then()
                .statusCode(HttpStatus.OK.value())

            RestAssured.given()
                .header(AuthenticationFilter.HEADER_LOGIN_ID, LOGIN_ID)
                .header(AuthenticationFilter.HEADER_LOGIN_PW, PASSWORD)
            .`when`()
                .post("/api/v1/products/$productId/likes")
            .then()
                .statusCode(HttpStatus.OK.value())
        }

        @Test
        fun `인증 없이 좋아요를 추가하면 401 에러가 발생한다`() {
            RestAssured.given()
            .`when`()
                .post("/api/v1/products/1/likes")
            .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("UNAUTHORIZED"))
        }

        @Test
        fun `삭제된 상품에 좋아요를 추가하면 404 에러가 발생한다`() {
            registerUser()
            val brandId = createBrand()
            val productId = createProduct(brandId)
            deleteProduct(productId)

            RestAssured.given()
                .header(AuthenticationFilter.HEADER_LOGIN_ID, LOGIN_ID)
                .header(AuthenticationFilter.HEADER_LOGIN_PW, PASSWORD)
            .`when`()
                .post("/api/v1/products/$productId/likes")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("NOT_FOUND"))
        }
    }

    @Nested
    inner class RemoveLike {
        @Test
        fun `정상적인 경우 좋아요가 취소된다`() {
            registerUser()
            val brandId = createBrand()
            val productId = createProduct(brandId)
            addLike(productId)

            RestAssured.given()
                .header(AuthenticationFilter.HEADER_LOGIN_ID, LOGIN_ID)
                .header(AuthenticationFilter.HEADER_LOGIN_PW, PASSWORD)
            .`when`()
                .delete("/api/v1/products/$productId/likes")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("meta.result", equalTo("SUCCESS"))
        }

        @Test
        fun `없는 좋아요를 취소해도 멱등하게 성공한다`() {
            registerUser()
            val brandId = createBrand()
            val productId = createProduct(brandId)

            RestAssured.given()
                .header(AuthenticationFilter.HEADER_LOGIN_ID, LOGIN_ID)
                .header(AuthenticationFilter.HEADER_LOGIN_PW, PASSWORD)
            .`when`()
                .delete("/api/v1/products/$productId/likes")
            .then()
                .statusCode(HttpStatus.OK.value())
        }

        @Test
        fun `인증 없이 좋아요를 취소하면 401 에러가 발생한다`() {
            RestAssured.given()
            .`when`()
                .delete("/api/v1/products/1/likes")
            .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("UNAUTHORIZED"))
        }
    }

    @Nested
    inner class GetMyLikes {
        @Test
        fun `좋아요한 목록을 조회할 수 있다`() {
            registerUser()
            val brandId = createBrand()
            val productId = createProduct(brandId)
            addLike(productId)

            RestAssured.given()
                .header(AuthenticationFilter.HEADER_LOGIN_ID, LOGIN_ID)
                .header(AuthenticationFilter.HEADER_LOGIN_PW, PASSWORD)
            .`when`()
                .get("/api/v1/me/likes")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("meta.result", equalTo("SUCCESS"))
                .body("data", hasSize<Any>(1))
        }

        @Test
        fun `좋아요가 없으면 빈 목록을 반환한다`() {
            registerUser()

            RestAssured.given()
                .header(AuthenticationFilter.HEADER_LOGIN_ID, LOGIN_ID)
                .header(AuthenticationFilter.HEADER_LOGIN_PW, PASSWORD)
            .`when`()
                .get("/api/v1/me/likes")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("data", hasSize<Any>(0))
        }

        @Test
        fun `인증 없이 조회하면 401 에러가 발생한다`() {
            RestAssured.given()
            .`when`()
                .get("/api/v1/me/likes")
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

    private fun deleteProduct(productId: Long) {
        RestAssured.given()
        .`when`()
            .delete("/api-admin/v1/products/$productId")
        .then()
            .statusCode(HttpStatus.OK.value())
    }

    private fun addLike(productId: Long) {
        RestAssured.given()
            .header(AuthenticationFilter.HEADER_LOGIN_ID, LOGIN_ID)
            .header(AuthenticationFilter.HEADER_LOGIN_PW, PASSWORD)
        .`when`()
            .post("/api/v1/products/$productId/likes")
        .then()
            .statusCode(HttpStatus.OK.value())
    }

    companion object {
        private const val LOGIN_ID = "testuser"
        private const val PASSWORD = "Password1!"
    }
}
