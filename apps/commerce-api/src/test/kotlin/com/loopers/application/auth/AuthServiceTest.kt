package com.loopers.application.auth

import com.loopers.application.error.ApplicationException
import com.loopers.application.member.MemberService
import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType
import com.loopers.domain.member.MemberModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import java.time.LocalDate

class AuthServiceTest {

    private lateinit var authService: AuthService
    private lateinit var memberService: MemberService
    private lateinit var cacheManager: ConcurrentMapCacheManager

    private val testMember = MemberModel(
        id = 1L,
        loginId = "testuser01",
        password = "encodedPassword",
        name = "홍길동",
        email = "test@example.com",
        birthday = LocalDate.of(1990, 1, 1),
    )

    @BeforeEach
    fun setUp() {
        memberService = mockk()
        cacheManager = ConcurrentMapCacheManager(AuthService.AUTH_CACHE)
        authService = AuthService(memberService, cacheManager)
    }

    @DisplayName("인증 및 캐싱 동작 검증")
    @Nested
    inner class Authenticate {

        @DisplayName("첫 번째 인증 요청 시, memberService.authenticate()를 호출하고 결과를 캐싱한다.")
        @Test
        fun callsAuthenticateAndCachesResult_onFirstRequest() {
            // arrange
            every { memberService.authenticate("testuser01", "TestPass123!") } returns testMember

            // act
            val result = authService.authenticate("testuser01", "TestPass123!")

            // assert
            verify(exactly = 1) { memberService.authenticate("testuser01", "TestPass123!") }
            assertThat(result.id).isEqualTo(1L)
            assertThat(result.loginId).isEqualTo("testuser01")
            val cached = cacheManager.getCache(AuthService.AUTH_CACHE)?.get("testuser01", CachedAuth::class.java)
            assertThat(cached).isNotNull
        }

        @DisplayName("동일한 자격증명으로 두 번째 요청 시, memberService.authenticate()를 호출하지 않고 캐시를 사용한다.")
        @Test
        fun usesCacheWithoutCallingAuthenticate_onSecondRequestWithSameCredentials() {
            // arrange
            every { memberService.authenticate("testuser01", "TestPass123!") } returns testMember

            // act
            authService.authenticate("testuser01", "TestPass123!")
            val result = authService.authenticate("testuser01", "TestPass123!")

            // assert
            verify(exactly = 1) { memberService.authenticate("testuser01", "TestPass123!") }
            assertThat(result.id).isEqualTo(1L)
            assertThat(result.loginId).isEqualTo("testuser01")
        }

        @DisplayName("캐시된 loginId에 다른 비밀번호로 요청하면, memberService.authenticate()를 다시 호출한다.")
        @Test
        fun callsAuthenticateAgain_whenDifferentPasswordForCachedLoginId() {
            // arrange
            every { memberService.authenticate("testuser01", "TestPass123!") } returns testMember
            every { memberService.authenticate("testuser01", "WrongPass456!") } throws
                CoreException(ErrorType.UNAUTHORIZED, "인증에 실패했습니다.")

            // act
            authService.authenticate("testuser01", "TestPass123!")

            // assert
            assertThrows<ApplicationException> {
                authService.authenticate("testuser01", "WrongPass456!")
            }
            verify(exactly = 1) { memberService.authenticate("testuser01", "WrongPass456!") }
        }

        @DisplayName("캐시 evict 후, memberService.authenticate()를 다시 호출한다.")
        @Test
        fun callsAuthenticateAgain_afterCacheEviction() {
            // arrange
            every { memberService.authenticate("testuser01", "TestPass123!") } returns testMember

            // act
            authService.authenticate("testuser01", "TestPass123!")
            authService.evictAuthCache("testuser01")
            authService.authenticate("testuser01", "TestPass123!")

            // assert
            verify(exactly = 2) { memberService.authenticate("testuser01", "TestPass123!") }
        }

        @DisplayName("memberService에서 CoreException이 발생하면, ApplicationException으로 변환한다.")
        @Test
        fun throwsApplicationException_whenMemberServiceThrowsCoreException() {
            // arrange
            every { memberService.authenticate("testuser01", "WrongPass!") } throws
                CoreException(ErrorType.UNAUTHORIZED, "인증에 실패했습니다.")

            // act & assert
            assertThrows<ApplicationException> {
                authService.authenticate("testuser01", "WrongPass!")
            }
        }
    }
}
