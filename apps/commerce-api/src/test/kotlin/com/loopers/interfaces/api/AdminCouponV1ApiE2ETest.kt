package com.loopers.interfaces.api

import com.loopers.domain.admin.Admin
import com.loopers.domain.coupon.CouponTemplate
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.coupon.IssuedCoupon
import com.loopers.infrastructure.admin.AdminJpaRepository
import com.loopers.infrastructure.coupon.CouponTemplateJpaRepository
import com.loopers.infrastructure.coupon.IssuedCouponJpaRepository
import com.loopers.interfaces.api.admin.coupon.AdminCouponV1Dto
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
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
import java.time.ZonedDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminCouponV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val adminJpaRepository: AdminJpaRepository,
    private val couponTemplateJpaRepository: CouponTemplateJpaRepository,
    private val issuedCouponJpaRepository: IssuedCouponJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    private lateinit var adminHeaders: HttpHeaders

    @BeforeEach
    fun setUp() {
        adminJpaRepository.save(Admin(ldap = "loopers.admin", name = "관리자"))
        adminHeaders = HttpHeaders()
        adminHeaders.set("X-Loopers-Ldap", "loopers.admin")
    }

    @AfterEach
    fun cleanUp() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("인증되지 않은 요청은 401 UNAUTHORIZED 응답을 받는다.")
    @Test
    fun returnsUnauthorized_whenNoLdapHeader() {
        // act
        val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
        val response = testRestTemplate.exchange("/api-admin/v1/coupons", HttpMethod.GET, null, responseType)

        // assert
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @DisplayName("POST /api-admin/v1/coupons")
    @Nested
    inner class CreateCouponTemplate {
        @DisplayName("유효한 쿠폰 템플릿 정보가 주어지면, 쿠폰 템플릿을 생성한다.")
        @Test
        fun createsCouponTemplate_whenValidRequest() {
            // arrange
            val req = AdminCouponV1Dto.CreateCouponTemplateRequest(
                name = "1000원 할인",
                type = CouponType.FIXED,
                value = 1000,
                minOrderAmount = 5000,
                expiredAt = ZonedDateTime.now().plusDays(30),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<AdminCouponV1Dto.CouponTemplateResponse>>() {}
            val response = testRestTemplate.exchange("/api-admin/v1/coupons", HttpMethod.POST, HttpEntity(req, adminHeaders), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(response.body?.data?.name).isEqualTo("1000원 할인") },
                { assertThat(response.body?.data?.type).isEqualTo(CouponType.FIXED) },
                { assertThat(response.body?.data?.value).isEqualTo(1000) },
                { assertThat(response.body?.data?.minOrderAmount).isEqualTo(5000) },
            )
        }
    }

    @DisplayName("GET /api-admin/v1/coupons")
    @Nested
    inner class GetCouponTemplates {
        @DisplayName("쿠폰 템플릿 목록을 조회한다.")
        @Test
        fun returnsCouponTemplateList() {
            // arrange
            couponTemplateJpaRepository.save(
                CouponTemplate(name = "1000원 할인", type = CouponType.FIXED, value = 1000, expiredAt = ZonedDateTime.now().plusDays(30)),
            )
            couponTemplateJpaRepository.save(
                CouponTemplate(name = "10% 할인", type = CouponType.RATE, value = 10, expiredAt = ZonedDateTime.now().plusDays(30)),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<PageResponse<AdminCouponV1Dto.CouponTemplateResponse>>>() {}
            val response = testRestTemplate.exchange("/api-admin/v1/coupons", HttpMethod.GET, HttpEntity<Any>(null, adminHeaders), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.content).hasSize(2) },
            )
        }
    }

    @DisplayName("GET /api-admin/v1/coupons/{couponId}")
    @Nested
    inner class GetCouponTemplate {
        @DisplayName("존재하는 쿠폰 템플릿 ID로 조회하면, 상세 정보를 반환한다.")
        @Test
        fun returnsCouponTemplate_whenExists() {
            // arrange
            val template = couponTemplateJpaRepository.save(
                CouponTemplate(name = "1000원 할인", type = CouponType.FIXED, value = 1000, expiredAt = ZonedDateTime.now().plusDays(30)),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<AdminCouponV1Dto.CouponTemplateResponse>>() {}
            val response = testRestTemplate.exchange("/api-admin/v1/coupons/${template.id}", HttpMethod.GET, HttpEntity<Any>(null, adminHeaders), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.name).isEqualTo("1000원 할인") },
            )
        }
    }

    @DisplayName("PUT /api-admin/v1/coupons/{couponId}")
    @Nested
    inner class UpdateCouponTemplate {
        @DisplayName("유효한 수정 정보가 주어지면, 쿠폰 템플릿을 수정한다.")
        @Test
        fun updatesCouponTemplate_whenValidRequest() {
            // arrange
            val template = couponTemplateJpaRepository.save(
                CouponTemplate(name = "기존 쿠폰", type = CouponType.FIXED, value = 1000, expiredAt = ZonedDateTime.now().plusDays(30)),
            )
            val req = AdminCouponV1Dto.UpdateCouponTemplateRequest(
                name = "수정된 쿠폰",
                value = 2000,
                minOrderAmount = 10000,
                expiredAt = ZonedDateTime.now().plusDays(60),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<AdminCouponV1Dto.CouponTemplateResponse>>() {}
            val response = testRestTemplate.exchange("/api-admin/v1/coupons/${template.id}", HttpMethod.PUT, HttpEntity(req, adminHeaders), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.name).isEqualTo("수정된 쿠폰") },
                { assertThat(response.body?.data?.value).isEqualTo(2000) },
            )
        }
    }

    @DisplayName("DELETE /api-admin/v1/coupons/{couponId}")
    @Nested
    inner class DeleteCouponTemplate {
        @DisplayName("존재하는 쿠폰 템플릿을 삭제한다.")
        @Test
        fun deletesCouponTemplate() {
            // arrange
            val template = couponTemplateJpaRepository.save(
                CouponTemplate(name = "삭제할 쿠폰", type = CouponType.FIXED, value = 1000, expiredAt = ZonedDateTime.now().plusDays(30)),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange("/api-admin/v1/coupons/${template.id}", HttpMethod.DELETE, HttpEntity<Any>(null, adminHeaders), responseType)

            // assert
            assertThat(response.statusCode.is2xxSuccessful).isTrue()
        }
    }

    @DisplayName("GET /api-admin/v1/coupons/{couponId}/issues")
    @Nested
    inner class GetIssuedCoupons {
        @DisplayName("발급 내역을 조회한다.")
        @Test
        fun returnsIssuedCouponList() {
            // arrange
            val template = couponTemplateJpaRepository.save(
                CouponTemplate(name = "1000원 할인", type = CouponType.FIXED, value = 1000, expiredAt = ZonedDateTime.now().plusDays(30)),
            )
            issuedCouponJpaRepository.save(IssuedCoupon(userId = 1L, couponTemplateId = template.id))
            issuedCouponJpaRepository.save(IssuedCoupon(userId = 2L, couponTemplateId = template.id))

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<PageResponse<AdminCouponV1Dto.IssuedCouponResponse>>>() {}
            val response = testRestTemplate.exchange("/api-admin/v1/coupons/${template.id}/issues", HttpMethod.GET, HttpEntity<Any>(null, adminHeaders), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.content).hasSize(2) },
            )
        }
    }
}
