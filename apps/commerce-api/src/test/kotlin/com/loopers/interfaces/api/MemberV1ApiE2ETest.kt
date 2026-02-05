package com.loopers.interfaces.api

import com.loopers.domain.member.MemberModel
import com.loopers.infrastructure.member.MemberJpaRepository
import com.loopers.interfaces.api.member.MemberV1Dto
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
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
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MemberV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val memberJpaRepository: MemberJpaRepository,
    private val passwordEncoder: PasswordEncoder,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ENDPOINT_REGISTER = "/api/v1/members"
        private const val ENDPOINT_ME = "/api/v1/members/me"
        private const val ENDPOINT_CHANGE_PASSWORD = "/api/v1/members/me/password"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("POST /api/v1/members - 회원가입")
    @Nested
    inner class Register {
        @DisplayName("유효한 회원정보로 등록하면, 회원 정보를 반환한다.")
        @Test
        fun registersMember_whenValidRequestIsProvided() {
            // arrange
            val request = MemberV1Dto.RegisterRequest(
                loginId = "testuser01",
                password = "TestPass123!",
                name = "홍길동",
                email = "test@example.com",
                birthDate = LocalDate.of(1990, 1, 1),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.RegisterResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_REGISTER,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.loginId).isEqualTo(request.loginId) },
                { assertThat(response.body?.data?.name).isEqualTo(request.name) },
                { assertThat(response.body?.data?.email).isEqualTo(request.email) },
                { assertThat(response.body?.data?.birthDate).isEqualTo(request.birthDate) },
            )
        }

        @DisplayName("중복된 loginId로 등록하면, 409 CONFLICT 응답을 받는다.")
        @Test
        fun returnsConflict_whenDuplicateLoginIdIsProvided() {
            // arrange
            val existingMember = createMember("testuser01", "TestPass123!")
            memberJpaRepository.save(existingMember)

            val request = MemberV1Dto.RegisterRequest(
                loginId = "testuser01",
                password = "TestPass456!",
                name = "김철수",
                email = "another@example.com",
                birthDate = LocalDate.of(1995, 5, 5),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.RegisterResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_REGISTER,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        }

        @DisplayName("잘못된 이메일 형식으로 등록하면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenInvalidEmailIsProvided() {
            // arrange
            val request = MemberV1Dto.RegisterRequest(
                loginId = "testuser01",
                password = "TestPass123!",
                name = "홍길동",
                email = "invalid-email",
                birthDate = LocalDate.of(1990, 1, 1),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.RegisterResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_REGISTER,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @DisplayName("잘못된 비밀번호 형식으로 등록하면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenInvalidPasswordIsProvided() {
            // arrange
            val request = MemberV1Dto.RegisterRequest(
                loginId = "testuser01",
                password = "short",
                name = "홍길동",
                email = "test@example.com",
                birthDate = LocalDate.of(1990, 1, 1),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.RegisterResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_REGISTER,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @DisplayName("GET /api/v1/members/me - 내 정보 조회")
    @Nested
    inner class GetMe {
        @DisplayName("유효한 인증 헤더로 조회하면, 마스킹된 이름과 함께 회원 정보를 반환한다.")
        @Test
        fun returnsMemberInfo_whenValidHeadersAreProvided() {
            // arrange
            val rawPassword = "TestPass123!"
            val member = createMember("testuser01", rawPassword)
            memberJpaRepository.save(member)

            val headers = HttpHeaders().apply {
                set("X-Loopers-LoginId", member.loginId)
                set("X-Loopers-LoginPw", rawPassword)
            }

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.MemberResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_ME,
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.loginId).isEqualTo(member.loginId) },
                { assertThat(response.body?.data?.name).isEqualTo("홍길*") },
                { assertThat(response.body?.data?.email).isEqualTo(member.email) },
                { assertThat(response.body?.data?.birthDate).isEqualTo(member.birthDate) },
            )
        }

        @DisplayName("존재하지 않는 loginId로 조회하면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenInvalidLoginIdIsProvided() {
            // arrange
            val headers = HttpHeaders().apply {
                set("X-Loopers-LoginId", "nonexistent")
                set("X-Loopers-LoginPw", "TestPass123!")
            }

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.MemberResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_ME,
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @DisplayName("잘못된 비밀번호로 조회하면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenIncorrectPasswordIsProvided() {
            // arrange
            val rawPassword = "TestPass123!"
            val member = createMember("testuser01", rawPassword)
            memberJpaRepository.save(member)

            val headers = HttpHeaders().apply {
                set("X-Loopers-LoginId", member.loginId)
                set("X-Loopers-LoginPw", "WrongPass456!")
            }

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.MemberResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_ME,
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @DisplayName("인증 헤더가 없으면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenHeadersAreMissing() {
            // arrange & act
            val responseType = object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.MemberResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_ME,
                HttpMethod.GET,
                HttpEntity<Any>(HttpHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @DisplayName("PUT /api/v1/members/me/password - 비밀번호 수정")
    @Nested
    inner class ChangePassword {
        @DisplayName("유효한 요청으로 비밀번호를 변경하면, 성공한다.")
        @Test
        fun changesPasswordSuccessfully_whenValidRequestIsProvided() {
            // arrange
            val rawPassword = "TestPass123!"
            val member = createMember("testuser01", rawPassword)
            memberJpaRepository.save(member)

            val headers = HttpHeaders().apply {
                set("X-Loopers-LoginId", member.loginId)
                set("X-Loopers-LoginPw", rawPassword)
            }
            val request = MemberV1Dto.ChangePasswordRequest(
                currentPassword = rawPassword,
                newPassword = "NewPass456!",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_CHANGE_PASSWORD,
                HttpMethod.PUT,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                {
                    val updatedMember = memberJpaRepository.findByLoginId(member.loginId)!!
                    assertThat(passwordEncoder.matches("NewPass456!", updatedMember.password)).isTrue()
                },
            )
        }

        @DisplayName("잘못된 현재 비밀번호로 변경하면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenIncorrectCurrentPasswordIsProvided() {
            // arrange
            val rawPassword = "TestPass123!"
            val member = createMember("testuser01", rawPassword)
            memberJpaRepository.save(member)

            val headers = HttpHeaders().apply {
                set("X-Loopers-LoginId", member.loginId)
                set("X-Loopers-LoginPw", rawPassword)
            }
            val request = MemberV1Dto.ChangePasswordRequest(
                currentPassword = "WrongPass456!",
                newPassword = "NewPass789!",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_CHANGE_PASSWORD,
                HttpMethod.PUT,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @DisplayName("유효하지 않은 새 비밀번호로 변경하면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenInvalidNewPasswordIsProvided() {
            // arrange
            val rawPassword = "TestPass123!"
            val member = createMember("testuser01", rawPassword)
            memberJpaRepository.save(member)

            val headers = HttpHeaders().apply {
                set("X-Loopers-LoginId", member.loginId)
                set("X-Loopers-LoginPw", rawPassword)
            }
            val request = MemberV1Dto.ChangePasswordRequest(
                currentPassword = rawPassword,
                newPassword = "short",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_CHANGE_PASSWORD,
                HttpMethod.PUT,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @DisplayName("인증 헤더가 없으면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenHeadersAreMissing() {
            // arrange
            val request = MemberV1Dto.ChangePasswordRequest(
                currentPassword = "TestPass123!",
                newPassword = "NewPass456!",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_CHANGE_PASSWORD,
                HttpMethod.PUT,
                HttpEntity(request),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    private fun createMember(loginId: String, rawPassword: String): MemberModel {
        val member = MemberModel(
            loginId = loginId,
            password = rawPassword,
            name = "홍길동",
            email = "test@example.com",
            birthDate = LocalDate.of(1990, 1, 1),
        )
        member.encryptPassword(passwordEncoder.encode(rawPassword))
        return member
    }
}
