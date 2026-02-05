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
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MemberV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val memberJpaRepository: MemberJpaRepository,
    private val passwordEncoder: PasswordEncoder,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ENDPOINT_REGISTER = "/api/v1/members/register"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("POST /api/v1/members/register")
    @Nested
    inner class Register {

        @DisplayName("유효한 정보로 회원가입 요청하면, 201 Created 응답을 받는다.")
        @Test
        fun returnsCreated_whenValidRequestIsProvided() {
            // arrange
            val request = MemberV1Dto.RegisterRequest(
                loginId = "testuser",
                password = "Test1234!",
                name = "홍길동",
                birthDate = "19900101",
                email = "test@example.com",
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
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(response.body?.data?.loginId).isEqualTo(request.loginId) },
                { assertThat(response.body?.data?.name).isEqualTo(request.name) },
                { assertThat(response.body?.data?.email).isEqualTo(request.email) },
            )
        }

        @DisplayName("유효한 정보로 회원가입하면, 비밀번호가 암호화되어 저장된다.")
        @Test
        fun encryptsPassword_whenValidRequestIsProvided() {
            // arrange
            val rawPassword = "Test1234!"
            val request = MemberV1Dto.RegisterRequest(
                loginId = "testuser",
                password = rawPassword,
                name = "홍길동",
                birthDate = "19900101",
                email = "test@example.com",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.RegisterResponse>>() {}
            testRestTemplate.exchange(
                ENDPOINT_REGISTER,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // assert
            val savedMember = memberJpaRepository.findByLoginId(request.loginId)
            assertThat(savedMember).isNotNull
            assertThat(savedMember!!.password).isNotEqualTo(rawPassword)
            assertThat(passwordEncoder.matches(rawPassword, savedMember.password)).isTrue()
        }

        @DisplayName("이미 존재하는 로그인 ID로 가입 요청하면, 400 Bad Request 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenLoginIdAlreadyExists() {
            // arrange
            val existingLoginId = "existinguser"
            memberJpaRepository.save(
                MemberModel(
                    loginId = existingLoginId,
                    password = passwordEncoder.encode("Test1234!"),
                    name = "기존회원",
                    birthDate = "19850101",
                    email = "existing@example.com",
                ),
            )

            val request = MemberV1Dto.RegisterRequest(
                loginId = existingLoginId,
                password = "NewPass123!",
                name = "신규회원",
                birthDate = "19900101",
                email = "new@example.com",
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

        @DisplayName("잘못된 이메일 형식으로 요청하면, 400 Bad Request 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenEmailFormatIsInvalid() {
            // arrange
            val request = MemberV1Dto.RegisterRequest(
                loginId = "testuser",
                password = "Test1234!",
                name = "홍길동",
                birthDate = "19900101",
                email = "invalid-email",
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

        @DisplayName("비밀번호에 생년월일이 포함되면, 400 Bad Request 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenPasswordContainsBirthDate() {
            // arrange
            val birthDate = "19900101"
            val request = MemberV1Dto.RegisterRequest(
                loginId = "testuser",
                password = "Test$birthDate!",
                name = "홍길동",
                birthDate = birthDate,
                email = "test@example.com",
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

        @DisplayName("비밀번호가 8자 미만이면, 400 Bad Request 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenPasswordIsTooShort() {
            // arrange
            val request = MemberV1Dto.RegisterRequest(
                loginId = "testuser",
                password = "Test12!",
                name = "홍길동",
                birthDate = "19900101",
                email = "test@example.com",
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
}
