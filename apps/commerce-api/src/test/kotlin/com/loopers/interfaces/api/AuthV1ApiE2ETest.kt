package com.loopers.interfaces.api

import com.loopers.domain.member.MemberModel
import com.loopers.domain.member.vo.BirthDate
import com.loopers.domain.member.vo.Email
import com.loopers.domain.member.vo.LoginId
import com.loopers.domain.member.vo.Name
import com.loopers.domain.member.vo.Password
import com.loopers.infrastructure.member.MemberJpaRepository
import com.loopers.interfaces.api.auth.AuthV1Dto
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val memberJpaRepository: MemberJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Nested
    inner class Signup {
        @Test
        fun `회원가입에_성공한다`() {
            // arrange
            val request = AuthV1Dto.SignupRequest(
                loginId = "newuser123",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )
            val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
            val httpEntity = HttpEntity(request, headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<AuthV1Dto.SignupResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/auth/signup",
                HttpMethod.POST,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.loginId).isEqualTo("newuser123") },
                { assertThat(response.body?.data?.name).isEqualTo("홍길동") },
                { assertThat(response.body?.data?.email).isEqualTo("test@example.com") },
            )
        }

        @Test
        fun `중복_로그인ID면_409_CONFLICT를_반환한다`() {
            // arrange
            createAndSaveMember(loginId = "existinguser")
            val request = AuthV1Dto.SignupRequest(
                loginId = "existinguser",
                password = "Password1!",
                name = "새회원",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "new@example.com",
            )
            val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
            val httpEntity = HttpEntity(request, headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/auth/signup",
                HttpMethod.POST,
                httpEntity,
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        }

        @Test
        fun `유효하지_않은_비밀번호면_400_BAD_REQUEST를_반환한다`() {
            // arrange
            val request = AuthV1Dto.SignupRequest(
                loginId = "newuser123",
                // 8자 미만
                password = "short",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            )
            val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
            val httpEntity = HttpEntity(request, headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/auth/signup",
                HttpMethod.POST,
                httpEntity,
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    private fun createAndSaveMember(
        loginId: String = "testuser123",
        rawPassword: String = "Password1!",
        birthDate: LocalDate = LocalDate.of(1990, 1, 15),
    ): MemberModel {
        return memberJpaRepository.save(
            MemberModel(
                loginId = LoginId(loginId),
                password = Password.of(rawPassword, birthDate),
                name = Name("홍길동"),
                birthDate = BirthDate(birthDate),
                email = Email("test@example.com"),
            ),
        )
    }
}
