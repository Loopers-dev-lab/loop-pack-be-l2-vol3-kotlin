package com.loopers.interfaces.api.admin.v1.coupon

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
import java.time.ZonedDateTime

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
class AdminCouponApiE2ETest {

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
    }

    @Nested
    inner class Create {
        @Test
        fun `쿠폰을 생성할 수 있다`() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body(createCouponRequest())
            .`when`()
                .post("/api-admin/v1/coupons")
            .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("meta.result", equalTo("SUCCESS"))
                .body("data.id", notNullValue())
        }

        @Test
        fun `쿠폰명이 비어있으면 400 에러가 발생한다`() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body(createCouponRequest(name = ""))
            .`when`()
                .post("/api-admin/v1/coupons")
            .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("meta.result", equalTo("FAIL"))
        }
    }

    @Nested
    inner class GetAll {
        @Test
        fun `쿠폰 목록을 조회할 수 있다`() {
            createCoupon()

            RestAssured.given()
            .`when`()
                .get("/api-admin/v1/coupons")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("meta.result", equalTo("SUCCESS"))
                .body("data.size()", greaterThanOrEqualTo(1))
        }
    }

    @Nested
    inner class GetById {
        @Test
        fun `쿠폰 상세를 조회할 수 있다`() {
            val couponId = createCoupon()

            RestAssured.given()
            .`when`()
                .get("/api-admin/v1/coupons/$couponId")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("meta.result", equalTo("SUCCESS"))
                .body("data.id", equalTo(couponId.toInt()))
                .body("data.name", equalTo("테스트쿠폰"))
        }

        @Test
        fun `존재하지 않는 쿠폰을 조회하면 404를 반환한다`() {
            RestAssured.given()
            .`when`()
                .get("/api-admin/v1/coupons/9999")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("NOT_FOUND"))
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `쿠폰을 수정할 수 있다`() {
            val couponId = createCoupon()

            RestAssured.given()
                .contentType(ContentType.JSON)
                .body(createCouponRequest(name = "수정된쿠폰"))
            .`when`()
                .put("/api-admin/v1/coupons/$couponId")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("meta.result", equalTo("SUCCESS"))
                .body("data.name", equalTo("수정된쿠폰"))
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `쿠폰을 삭제할 수 있다`() {
            val couponId = createCoupon()

            RestAssured.given()
            .`when`()
                .delete("/api-admin/v1/coupons/$couponId")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("meta.result", equalTo("SUCCESS"))
        }
    }

    @Nested
    inner class GetIssuedCoupons {
        @Test
        fun `쿠폰 발급 내역을 조회할 수 있다`() {
            registerUser()
            val couponId = createCoupon()
            issueCoupon(couponId)

            RestAssured.given()
            .`when`()
                .get("/api-admin/v1/coupons/$couponId/issued")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("meta.result", equalTo("SUCCESS"))
                .body("data.size()", equalTo(1))
        }
    }

    private fun createCoupon(): Long {
        return RestAssured.given()
            .contentType(ContentType.JSON)
            .body(createCouponRequest())
        .`when`()
            .post("/api-admin/v1/coupons")
        .then()
            .statusCode(HttpStatus.CREATED.value())
            .extract()
            .jsonPath()
            .getLong("data.id")
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

    private fun issueCoupon(couponId: Long) {
        RestAssured.given()
            .header("X-Loopers-LoginId", LOGIN_ID)
            .header("X-Loopers-LoginPw", PASSWORD)
        .`when`()
            .post("/api/v1/coupons/$couponId/issue")
        .then()
            .statusCode(HttpStatus.CREATED.value())
    }

    private fun createCouponRequest(
        name: String = "테스트쿠폰",
    ) = mapOf(
        "name" to name,
        "discountType" to "FIXED",
        "discountValue" to 3000,
        "minOrderAmount" to 10000,
        "maxIssueCount" to 100,
        "expiredAt" to ZonedDateTime.now().plusDays(30).toString(),
    )

    companion object {
        private const val LOGIN_ID = "testuser"
        private const val PASSWORD = "Password1!"
    }
}
