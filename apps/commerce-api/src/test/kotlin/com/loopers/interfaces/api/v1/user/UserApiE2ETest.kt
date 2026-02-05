package com.loopers.interfaces.api.v1.user

import com.loopers.interfaces.api.auth.AuthenticationFilter
import com.loopers.testcontainers.MySqlTestContainersConfig
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers.containsInAnyOrder
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
@Sql(statements = ["DELETE FROM users"], executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class UserApiE2ETest {

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
    }

    @Nested
    inner class Register {
        @Test
        fun `유효한 요청이면 회원가입이 성공해야 한다`() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body(createUserRequest())
            .`when`()
                .post("/api/v1/users")
            .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("meta.result", equalTo("SUCCESS"))
                .body("data.id", notNullValue())
        }

        @Test
        fun `허용되지 않는 특수문자가 포함된 비밀번호는 400 에러가 발생한다`() {
            val invalidPasswords = listOf("Password1~", "Password1|", "Password1\\")

            invalidPasswords.forEach { invalidPassword ->
                RestAssured.given()
                    .contentType(ContentType.JSON)
                    .body(createUserRequest(password = invalidPassword))
                .`when`()
                    .post("/api/v1/users")
                .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value())
                    .body("meta.result", equalTo("FAIL"))
                    .body("meta.errorCode", equalTo("BAD_REQUEST"))
            }
        }

        @Test
        fun `성별이 없으면 400 에러가 발생한다`() {
            val requestWithoutGender = mapOf(
                "loginId" to LOGIN_ID,
                "password" to PASSWORD,
                "name" to NAME,
                "birthDate" to BIRTH_DATE,
                "email" to EMAIL,
            )

            RestAssured.given()
                .contentType(ContentType.JSON)
                .body(requestWithoutGender)
            .`when`()
                .post("/api/v1/users")
            .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("BAD_REQUEST"))
        }

        @Test
        fun `이미 존재하는 loginId로 가입하면 409 에러가 발생한다`() {
            registerUser()

            RestAssured.given()
                .contentType(ContentType.JSON)
                .body(createUserRequest())
            .`when`()
                .post("/api/v1/users")
            .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("CONFLICT"))
        }

        @Test
        fun `유효하지 않은 성별 값이면 400 에러가 발생한다`() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body(createUserRequest(gender = "INVALID"))
            .`when`()
                .post("/api/v1/users")
            .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("BAD_REQUEST"))
        }
    }

    @Nested
    inner class GetMyInfo {
        @Test
        fun `인증된 사용자는 내 정보를 조회할 수 있다`() {
            registerUser()

            RestAssured.given()
                .header(AuthenticationFilter.HEADER_LOGIN_ID, LOGIN_ID)
                .header(AuthenticationFilter.HEADER_LOGIN_PW, PASSWORD)
            .`when`()
                .get("/api/v1/users/me")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("meta.result", equalTo("SUCCESS"))
                .body("data.loginId", equalTo(LOGIN_ID))
                .body("data.name", equalTo("신형*"))
                .body("data.birthDate", equalTo(BIRTH_DATE))
                .body("data.email", equalTo(EMAIL))
                .body("data.gender", equalTo(GENDER))
        }

        @Test
        fun `내 정보 조회 응답은 요구사항에 명시된 필드만 포함한다`() {
            registerUser()

            RestAssured.given()
                .header(AuthenticationFilter.HEADER_LOGIN_ID, LOGIN_ID)
                .header(AuthenticationFilter.HEADER_LOGIN_PW, PASSWORD)
            .`when`()
                .get("/api/v1/users/me")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.keySet()", containsInAnyOrder("loginId", "name", "birthDate", "email", "gender"))
        }

        @Test
        fun `인증 헤더가 없으면 401 에러가 발생한다`() {
            RestAssured.given()
            .`when`()
                .get("/api/v1/users/me")
            .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("UNAUTHORIZED"))
        }

        @Test
        fun `비밀번호가 틀리면 401 에러가 발생한다`() {
            registerUser()

            RestAssured.given()
                .header(AuthenticationFilter.HEADER_LOGIN_ID, LOGIN_ID)
                .header(AuthenticationFilter.HEADER_LOGIN_PW, "WrongPass1!")
            .`when`()
                .get("/api/v1/users/me")
            .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("UNAUTHORIZED"))
        }
    }

    @Nested
    inner class ChangePassword {
        @Test
        fun `정상적인 경우 비밀번호가 변경되어야 한다`() {
            registerUser()

            RestAssured.given()
                .contentType(ContentType.JSON)
                .header(AuthenticationFilter.HEADER_LOGIN_ID, LOGIN_ID)
                .header(AuthenticationFilter.HEADER_LOGIN_PW, PASSWORD)
                .body(mapOf("oldPassword" to PASSWORD, "newPassword" to NEW_PASSWORD))
            .`when`()
                .put("/api/v1/users/password")
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("meta.result", equalTo("SUCCESS"))
                .body("data", equalTo(null))

            // 새 비밀번호로 인증 확인
            RestAssured.given()
                .header(AuthenticationFilter.HEADER_LOGIN_ID, LOGIN_ID)
                .header(AuthenticationFilter.HEADER_LOGIN_PW, NEW_PASSWORD)
            .`when`()
                .get("/api/v1/users/me")
            .then()
                .statusCode(HttpStatus.OK.value())
        }

        @Test
        fun `인증 헤더가 없으면 401 에러가 발생한다`() {
            RestAssured.given()
                .contentType(ContentType.JSON)
                .body(mapOf("oldPassword" to PASSWORD, "newPassword" to NEW_PASSWORD))
            .`when`()
                .put("/api/v1/users/password")
            .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("UNAUTHORIZED"))
        }

        @Test
        fun `기존 비밀번호가 틀리면 400 에러가 발생한다`() {
            registerUser()

            RestAssured.given()
                .contentType(ContentType.JSON)
                .header(AuthenticationFilter.HEADER_LOGIN_ID, LOGIN_ID)
                .header(AuthenticationFilter.HEADER_LOGIN_PW, PASSWORD)
                .body(mapOf("oldPassword" to "WrongPass1!", "newPassword" to NEW_PASSWORD))
            .`when`()
                .put("/api/v1/users/password")
            .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("BAD_REQUEST"))
        }

        @Test
        fun `새 비밀번호가 기존과 동일하면 400 에러가 발생한다`() {
            registerUser()

            RestAssured.given()
                .contentType(ContentType.JSON)
                .header(AuthenticationFilter.HEADER_LOGIN_ID, LOGIN_ID)
                .header(AuthenticationFilter.HEADER_LOGIN_PW, PASSWORD)
                .body(mapOf("oldPassword" to PASSWORD, "newPassword" to PASSWORD))
            .`when`()
                .put("/api/v1/users/password")
            .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("BAD_REQUEST"))
        }

        @Test
        fun `새 비밀번호에 생년월일이 포함되면 400 에러가 발생한다`() {
            registerUser()

            RestAssured.given()
                .contentType(ContentType.JSON)
                .header(AuthenticationFilter.HEADER_LOGIN_ID, LOGIN_ID)
                .header(AuthenticationFilter.HEADER_LOGIN_PW, PASSWORD)
                .body(mapOf("oldPassword" to PASSWORD, "newPassword" to "New19930401!"))
            .`when`()
                .put("/api/v1/users/password")
            .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("BAD_REQUEST"))
        }

        @Test
        fun `새 비밀번호가 8자 미만이면 400 에러가 발생한다`() {
            registerUser()

            RestAssured.given()
                .contentType(ContentType.JSON)
                .header(AuthenticationFilter.HEADER_LOGIN_ID, LOGIN_ID)
                .header(AuthenticationFilter.HEADER_LOGIN_PW, PASSWORD)
                .body(mapOf("oldPassword" to PASSWORD, "newPassword" to "Short1!"))
            .`when`()
                .put("/api/v1/users/password")
            .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("meta.result", equalTo("FAIL"))
                .body("meta.errorCode", equalTo("BAD_REQUEST"))
        }
    }

    private fun registerUser() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(createUserRequest())
        .`when`()
            .post("/api/v1/users")
        .then()
            .statusCode(HttpStatus.CREATED.value())
    }

    private fun createUserRequest(password: String = PASSWORD, gender: String = GENDER) = mapOf(
        "loginId" to LOGIN_ID,
        "password" to password,
        "name" to NAME,
        "birthDate" to BIRTH_DATE,
        "email" to EMAIL,
        "gender" to gender,
    )

    companion object {
        private const val LOGIN_ID = "tkaqkeldk"
        private const val PASSWORD = "Password1!"
        private const val NEW_PASSWORD = "NewPass2@"
        private const val NAME = "신형기"
        private const val BIRTH_DATE = "1993-04-01"
        private const val EMAIL = "tkaqkeldk99@gmail.com"
        private const val GENDER = "MALE"
    }
}
