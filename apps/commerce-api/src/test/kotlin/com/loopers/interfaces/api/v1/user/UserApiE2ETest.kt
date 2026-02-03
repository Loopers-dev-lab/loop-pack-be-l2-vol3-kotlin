package com.loopers.interfaces.api.v1.user

import com.loopers.testcontainers.MySqlTestContainersConfig
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.test.context.jdbc.Sql

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(MySqlTestContainersConfig::class)
@Sql(statements = ["DELETE FROM users"], executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class UserApiE2ETest {

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
    }

    @Test
    fun `유효한 요청이면 회원가입이 성공해야 한다`() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(
                mapOf(
                    "loginId" to "testuser",
                    "password" to "Password1!",
                    "name" to "테스트",
                    "birthDate" to "1990-01-01",
                    "email" to "test@example.com",
                    "gender" to "MALE",
                ),
            )
        .`when`()
            .post("/api/v1/users")
        .then()
            .statusCode(HttpStatus.CREATED.value())
            .body("meta.result", equalTo("SUCCESS"))
            .body("data.id", notNullValue())
    }
}
