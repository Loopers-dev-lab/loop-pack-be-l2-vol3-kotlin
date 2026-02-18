package com.loopers.interfaces.api.admin.v1.brand

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
class AdminBrandApiE2ETest {

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
    }

    @Nested
    inner class Create {
        @Test
        fun `정상적인 경우 브랜드가 생성된다`() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body(createBrandRequest())
            .`when`()
                .post("/api-admin/v1/brands")
            .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("meta.result", equalTo("SUCCESS"))
                .body("data.id", notNullValue())
        }

        @Test
        fun `이름이 빈값이면 400 에러가 발생한다`() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body(mapOf("name" to "", "description" to null, "logoUrl" to null))
            .`when`()
                .post("/api-admin/v1/brands")
            .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("BAD_REQUEST"))
        }

        @Test
        fun `중복된 브랜드명으로 생성하면 409 에러가 발생한다`() {
            createBrand()

            RestAssured.given()
                .contentType(ContentType.JSON)
                .body(createBrandRequest())
            .`when`()
                .post("/api-admin/v1/brands")
            .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("CONFLICT"))
        }
    }

    @Nested
    inner class GetAll {
        @Test
        fun `전체 브랜드 목록을 조회할 수 있다`() {
            createBrand()

            RestAssured.given()
            .`when`()
                .get("/api-admin/v1/brands")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("meta.result", equalTo("SUCCESS"))
                .body("data.size()", greaterThanOrEqualTo(1))
        }
    }

    @Nested
    inner class GetById {
        @Test
        fun `존재하는 브랜드를 조회할 수 있다`() {
            val brandId = createBrand()

            RestAssured.given()
            .`when`()
                .get("/api-admin/v1/brands/$brandId")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("meta.result", equalTo("SUCCESS"))
                .body("data.id", equalTo(brandId.toInt()))
        }

        @Test
        fun `존재하지 않는 브랜드를 조회하면 404를 반환한다`() {
            RestAssured.given()
            .`when`()
                .get("/api-admin/v1/brands/9999")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("NOT_FOUND"))
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `정상적인 경우 브랜드가 수정된다`() {
            val brandId = createBrand()

            RestAssured.given()
                .contentType(ContentType.JSON)
                .body(mapOf("name" to "수정된브랜드", "description" to "수정됨", "logoUrl" to null))
            .`when`()
                .put("/api-admin/v1/brands/$brandId")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("meta.result", equalTo("SUCCESS"))
                .body("data.name", equalTo("수정된브랜드"))
        }

        @Test
        fun `이름이 빈값이면 400 에러가 발생한다`() {
            val brandId = createBrand()

            RestAssured.given()
                .contentType(ContentType.JSON)
                .body(mapOf("name" to "", "description" to null, "logoUrl" to null))
            .`when`()
                .put("/api-admin/v1/brands/$brandId")
            .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("BAD_REQUEST"))
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `정상적인 경우 브랜드가 삭제된다`() {
            val brandId = createBrand()

            RestAssured.given()
            .`when`()
                .delete("/api-admin/v1/brands/$brandId")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("meta.result", equalTo("SUCCESS"))
        }

        @Test
        fun `존재하지 않는 브랜드를 삭제하면 404를 반환한다`() {
            RestAssured.given()
            .`when`()
                .delete("/api-admin/v1/brands/9999")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("NOT_FOUND"))
        }
    }

    private fun createBrand(): Long {
        return RestAssured.given()
            .contentType(ContentType.JSON)
            .body(createBrandRequest())
        .`when`()
            .post("/api-admin/v1/brands")
        .then()
            .statusCode(HttpStatus.CREATED.value())
            .extract()
            .jsonPath()
            .getLong("data.id")
    }

    private fun createBrandRequest() = mapOf(
        "name" to BRAND_NAME,
        "description" to "설명",
        "logoUrl" to null,
    )

    companion object {
        private const val BRAND_NAME = "테스트브랜드"
    }
}
