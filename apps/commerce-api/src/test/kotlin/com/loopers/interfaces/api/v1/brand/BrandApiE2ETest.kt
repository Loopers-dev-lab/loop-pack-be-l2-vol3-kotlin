package com.loopers.interfaces.api.v1.brand

import com.loopers.testcontainers.MySqlTestContainersConfig
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
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
@Sql(statements = ["DELETE FROM brand"], executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class BrandApiE2ETest {

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
    }

    @Nested
    inner class GetBrand {
        @Test
        fun `존재하는 브랜드를 정상 조회할 수 있다`() {
            val brandId = createBrand()

            RestAssured.given()
            .`when`()
                .get("/api/v1/brands/$brandId")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("meta.result", equalTo("SUCCESS"))
                .body("data.id", equalTo(brandId.toInt()))
                .body("data.name", equalTo(BRAND_NAME))
        }

        @Test
        fun `존재하지 않는 브랜드를 조회하면 404를 반환한다`() {
            RestAssured.given()
            .`when`()
                .get("/api/v1/brands/9999")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("NOT_FOUND"))
        }

        @Test
        fun `삭제된 브랜드를 조회하면 404를 반환한다`() {
            val brandId = createBrand()
            deleteBrand(brandId)

            RestAssured.given()
            .`when`()
                .get("/api/v1/brands/$brandId")
            .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("NOT_FOUND"))
        }
    }

    private fun createBrand(): Long {
        return RestAssured.given()
            .contentType(ContentType.JSON)
            .body(mapOf("name" to BRAND_NAME, "description" to "설명", "logoUrl" to null))
        .`when`()
            .post("/api-admin/v1/brands")
        .then()
            .statusCode(HttpStatus.CREATED.value())
            .body("data.id", notNullValue())
            .extract()
            .jsonPath()
            .getLong("data.id")
    }

    private fun deleteBrand(brandId: Long) {
        RestAssured.given()
        .`when`()
            .delete("/api-admin/v1/brands/$brandId")
        .then()
            .statusCode(HttpStatus.OK.value())
    }

    companion object {
        private const val BRAND_NAME = "테스트브랜드"
    }
}
