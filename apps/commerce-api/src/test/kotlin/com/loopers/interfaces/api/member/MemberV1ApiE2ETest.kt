package com.loopers.interfaces.api.member

import com.loopers.domain.auth.JwtTokenProvider
import com.loopers.domain.member.MemberModel
import com.loopers.infrastructure.member.BCryptPasswordEncoder
import com.loopers.infrastructure.member.MemberJpaRepository
import com.loopers.interfaces.api.ApiResponse
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
import org.springframework.http.MediaType
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MemberV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val memberJpaRepository: MemberJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    private val jwtTokenProvider: JwtTokenProvider,
    private val passwordEncoder: BCryptPasswordEncoder,
) {
    companion object {
        private const val ENDPOINT_SIGN_UP = "/api/v1/members/sign-up"
        private const val ENDPOINT_ME = "/api/v1/members/me"
        private const val ENDPOINT_CHANGE_PASSWORD = "/api/v1/members/me/password"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("GET /api/v1/members/me")
    @Nested
    inner class GetMyInfo {

        private fun createMemberAndGetToken(): Pair<MemberModel, String> {
            val member = memberJpaRepository.save(
                MemberModel(
                    loginId = "test_user1",
                    password = passwordEncoder.encode("Password1!"),
                    name = "홍길동",
                    birthDate = LocalDate.of(1990, 1, 15),
                    email = "hong@example.com",
                ),
            )
            val token = jwtTokenProvider.generateToken(member.id, member.loginId)
            return member to token
        }

        @DisplayName("유효한 토큰이면, 마스킹된 내 정보가 조회된다")
        @Test
        fun returnsMyInfo_whenValidTokenIsProvided() {
            // arrange
            val (_, token) = createMemberAndGetToken()
            val headers = HttpHeaders().apply {
                set("Authorization", "Bearer $token")
            }

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.MyInfoResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_ME,
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
                { assertThat(response.body?.data?.loginId).isEqualTo("test_user1") },
                { assertThat(response.body?.data?.name).isEqualTo("홍*동") },
                { assertThat(response.body?.data?.birthDate).isEqualTo(LocalDate.of(1990, 1, 15)) },
                { assertThat(response.body?.data?.email).isEqualTo("ho***@example.com") },
            )
        }

        @DisplayName("Authorization 헤더가 없으면, 401 UNAUTHORIZED 응답을 받는다")
        @Test
        fun returnsUnauthorized_whenNoAuthorizationHeader() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_ME,
                HttpMethod.GET,
                HttpEntity<Any>(HttpHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
                { assertThat(response.body?.meta?.errorCode).isEqualTo("UNAUTHORIZED") },
            )
        }

        @DisplayName("유효하지 않은 토큰이면, 401 INVALID_TOKEN 응답을 받는다")
        @Test
        fun returnsInvalidToken_whenTokenIsInvalid() {
            // arrange
            val headers = HttpHeaders().apply {
                set("Authorization", "Bearer invalid.token.here")
            }

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_ME,
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
                { assertThat(response.body?.meta?.errorCode).isEqualTo("INVALID_TOKEN") },
            )
        }

        @DisplayName("삭제된 회원의 토큰이면, 404 NOT_FOUND 응답을 받는다")
        @Test
        fun returnsNotFound_whenMemberDoesNotExist() {
            // arrange
            val token = jwtTokenProvider.generateToken(999L, "deleted_user")
            val headers = HttpHeaders().apply {
                set("Authorization", "Bearer $token")
            }

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_ME,
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }
    }

    @DisplayName("POST /api/v1/members/sign-up")
    @Nested
    inner class SignUp {
        @DisplayName("유효한 요청이면, 회원이 생성되고 200 OK 응답을 받는다")
        @Test
        fun returnsSuccess_whenValidRequestIsProvided() {
            // arrange
            val request = MemberV1Dto.SignUpRequest(
                loginId = "test_user1",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 5, 15),
                email = "test@example.com",
            )
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.SignUpResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_SIGN_UP,
                HttpMethod.POST,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.loginId).isEqualTo("test_user1") },
                { assertThat(response.body?.data?.name).isEqualTo("홍길동") },
                { assertThat(response.body?.data?.email).isEqualTo("test@example.com") },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
            )

            // verify saved member
            val savedMember = memberJpaRepository.findByLoginId("test_user1")
            assertThat(savedMember).isNotNull
            assertThat(savedMember?.password).isNotEqualTo("Password1!")
        }

        @DisplayName("이미 존재하는 로그인 ID로 요청하면, 409 CONFLICT 응답을 받는다")
        @Test
        fun returnsConflict_whenLoginIdAlreadyExists() {
            // arrange
            val existingMember = MemberModel(
                loginId = "test_user1",
                password = "\$2a\$10\$encodedPasswordHash",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 5, 15),
                email = "existing@example.com",
            )
            memberJpaRepository.save(existingMember)

            val request = MemberV1Dto.SignUpRequest(
                loginId = "test_user1",
                password = "Password1!",
                name = "김철수",
                birthDate = LocalDate.of(1995, 3, 20),
                email = "new@example.com",
            )
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.SignUpResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_SIGN_UP,
                HttpMethod.POST,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("비밀번호에 생년월일이 포함되면, 400 BAD_REQUEST 응답을 받는다")
        @Test
        fun returnsBadRequest_whenPasswordContainsBirthDate() {
            // arrange
            val request = mapOf(
                "loginId" to "test_user2",
                "password" to "19900515A!",
                "name" to "홍길동",
                "birthDate" to "1990-05-15",
                "email" to "test@example.com",
            )
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.SignUpResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_SIGN_UP,
                HttpMethod.POST,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("이메일 형식이 올바르지 않으면, 400 BAD_REQUEST 응답을 받는다")
        @Test
        fun returnsBadRequest_whenEmailFormatIsInvalid() {
            // arrange
            val request = mapOf(
                "loginId" to "test_user3",
                "password" to "Password1!",
                "name" to "홍길동",
                "birthDate" to "1990-05-15",
                "email" to "invalid-email",
            )
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.SignUpResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_SIGN_UP,
                HttpMethod.POST,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }
    }

    @DisplayName("PATCH /api/v1/members/me/password")
    @Nested
    inner class ChangePassword {

        private fun createMemberAndGetToken(
            rawPassword: String = "Password1!",
        ): Pair<MemberModel, String> {
            val member = memberJpaRepository.save(
                MemberModel(
                    loginId = "test_user1",
                    password = passwordEncoder.encode(rawPassword),
                    name = "홍길동",
                    birthDate = LocalDate.of(1990, 1, 15),
                    email = "hong@example.com",
                ),
            )
            val token = jwtTokenProvider.generateToken(member.id, member.loginId)
            return member to token
        }

        @DisplayName("유효한 토큰과 올바른 요청이면, 200 OK 응답을 받는다")
        @Test
        fun returnsSuccess_whenValidRequest() {
            // arrange
            val (_, token) = createMemberAndGetToken()
            val request = mapOf(
                "currentPassword" to "Password1!",
                "newPassword" to "NewPass1!x",
            )
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("Authorization", "Bearer $token")
            }

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_CHANGE_PASSWORD,
                HttpMethod.PATCH,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
                { assertThat(response.body?.data).isNull() },
            )

            // verify password changed
            val updatedMember = memberJpaRepository.findByLoginId("test_user1")
            assertThat(passwordEncoder.matches("NewPass1!x", updatedMember!!.password)).isTrue()
        }

        @DisplayName("Authorization 헤더가 없으면, 401 UNAUTHORIZED 응답을 받는다")
        @Test
        fun returnsUnauthorized_whenNoAuthorizationHeader() {
            // arrange
            val request = mapOf(
                "currentPassword" to "Password1!",
                "newPassword" to "NewPass1!x",
            )
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_CHANGE_PASSWORD,
                HttpMethod.PATCH,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
                { assertThat(response.body?.meta?.errorCode).isEqualTo("UNAUTHORIZED") },
            )
        }

        @DisplayName("현재 비밀번호가 일치하지 않으면, 401 UNAUTHORIZED 응답을 받는다")
        @Test
        fun returnsUnauthorized_whenCurrentPasswordDoesNotMatch() {
            // arrange
            val (_, token) = createMemberAndGetToken()
            val request = mapOf(
                "currentPassword" to "WrongPass1!",
                "newPassword" to "NewPass1!x",
            )
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("Authorization", "Bearer $token")
            }

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_CHANGE_PASSWORD,
                HttpMethod.PATCH,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
                { assertThat(response.body?.meta?.errorCode).isEqualTo("UNAUTHORIZED") },
            )
        }

        @DisplayName("현재 비밀번호와 새 비밀번호가 동일하면, 400 BAD_REQUEST 응답을 받는다")
        @Test
        fun returnsBadRequest_whenNewPasswordIsSameAsCurrent() {
            // arrange
            val (_, token) = createMemberAndGetToken()
            val request = mapOf(
                "currentPassword" to "Password1!",
                "newPassword" to "Password1!",
            )
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("Authorization", "Bearer $token")
            }

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_CHANGE_PASSWORD,
                HttpMethod.PATCH,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("비밀번호 규칙에 위반되면, 400 BAD_REQUEST 응답을 받는다")
        @Test
        fun returnsBadRequest_whenNewPasswordViolatesRules() {
            // arrange
            val (_, token) = createMemberAndGetToken()
            val request = mapOf(
                "currentPassword" to "Password1!",
                "newPassword" to "short",
            )
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("Authorization", "Bearer $token")
            }

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_CHANGE_PASSWORD,
                HttpMethod.PATCH,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }
    }
}
