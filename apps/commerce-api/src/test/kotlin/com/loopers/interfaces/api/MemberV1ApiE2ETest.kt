package com.loopers.interfaces.api

import com.loopers.domain.member.MemberModel
import com.loopers.domain.member.vo.BirthDate
import com.loopers.domain.member.vo.Email
import com.loopers.domain.member.vo.LoginId
import com.loopers.domain.member.vo.Name
import com.loopers.domain.member.vo.Password
import com.loopers.infrastructure.member.MemberJpaRepository
import com.loopers.interfaces.api.member.MemberV1Dto
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
class MemberV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val memberJpaRepository: MemberJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Nested
    inner class GetMyProfile {
        @Test
        fun `내_정보_조회에_성공한다`() {
            // arrange
            createAndSaveMember(loginId = "myuser", rawPassword = "Password1!", name = "홍길동")
            val headers = HttpHeaders().apply {
                set("X-Loopers-LoginId", "myuser")
                set("X-Loopers-LoginPw", "Password1!")
            }
            val httpEntity = HttpEntity<Any>(headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.MyProfileResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/members/me",
                HttpMethod.GET,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.loginId).isEqualTo("myuser") },
                { assertThat(response.body?.data?.email).isEqualTo("test@example.com") },
            )
        }

        @Test
        fun `인증_헤더가_없으면_400을_반환한다`() {
            // arrange
            val httpEntity = HttpEntity<Any>(HttpHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/members/me",
                HttpMethod.GET,
                httpEntity,
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        fun `이름의_마지막_글자가_마스킹되어_반환된다`() {
            // arrange
            createAndSaveMember(loginId = "maskuser", rawPassword = "Password1!", name = "김철수")
            val headers = HttpHeaders().apply {
                set("X-Loopers-LoginId", "maskuser")
                set("X-Loopers-LoginPw", "Password1!")
            }
            val httpEntity = HttpEntity<Any>(headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<MemberV1Dto.MyProfileResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/members/me",
                HttpMethod.GET,
                httpEntity,
                responseType,
            )

            // assert
            assertThat(response.body?.data?.name).isEqualTo("김철*")
        }
    }

    @Nested
    inner class ChangePassword {
        @Test
        fun `비밀번호_변경에_성공한다`() {
            // arrange
            createAndSaveMember(loginId = "pwuser", rawPassword = "OldPassword1!")
            val request = MemberV1Dto.ChangePasswordRequest(
                currentPassword = "OldPassword1!",
                newPassword = "NewPassword1!",
            )
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("X-Loopers-LoginId", "pwuser")
                set("X-Loopers-LoginPw", "OldPassword1!")
            }
            val httpEntity = HttpEntity(request, headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/members/me/password",
                HttpMethod.PATCH,
                httpEntity,
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)

            // 새 비밀번호로 인증 가능한지 확인
            val updatedMember = memberJpaRepository.findByLoginId(LoginId("pwuser"))
            assertThat(updatedMember?.password?.matches("NewPassword1!")).isTrue()
        }

        @Test
        fun `현재_비밀번호와_동일하면_400을_반환한다`() {
            // arrange
            val samePassword = "SamePassword1!"
            createAndSaveMember(loginId = "pwuser2", rawPassword = samePassword)
            val request = MemberV1Dto.ChangePasswordRequest(
                currentPassword = samePassword,
                newPassword = samePassword,
            )
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("X-Loopers-LoginId", "pwuser2")
                set("X-Loopers-LoginPw", samePassword)
            }
            val httpEntity = HttpEntity(request, headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/members/me/password",
                HttpMethod.PATCH,
                httpEntity,
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        fun `인증_실패하면_401을_반환한다`() {
            // arrange
            createAndSaveMember(loginId = "pwuser3", rawPassword = "Password1!")
            val request = MemberV1Dto.ChangePasswordRequest(
                currentPassword = "WrongPassword1!",
                newPassword = "NewPassword1!",
            )
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("X-Loopers-LoginId", "pwuser3")
                set("X-Loopers-LoginPw", "WrongPassword1!") // 틀린 비밀번호
            }
            val httpEntity = HttpEntity(request, headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/members/me/password",
                HttpMethod.PATCH,
                httpEntity,
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }

    private fun createAndSaveMember(
        loginId: String = "testuser123",
        rawPassword: String = "Password1!",
        name: String = "홍길동",
        birthDate: LocalDate = LocalDate.of(1990, 1, 15),
    ): MemberModel {
        return memberJpaRepository.save(
            MemberModel(
                loginId = LoginId(loginId),
                password = Password.of(rawPassword, birthDate),
                name = Name(name),
                birthDate = BirthDate(birthDate),
                email = Email("test@example.com"),
            ),
        )
    }
}
