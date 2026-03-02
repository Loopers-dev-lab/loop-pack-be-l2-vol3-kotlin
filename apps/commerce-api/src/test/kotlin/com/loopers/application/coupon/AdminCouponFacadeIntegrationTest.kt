package com.loopers.application.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponQuantity
import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.Discount
import com.loopers.domain.coupon.DiscountType
import com.loopers.domain.coupon.IssuedCoupon
import com.loopers.domain.coupon.IssuedCouponRepository
import com.loopers.domain.coupon.IssuedCouponStatus
import com.loopers.domain.user.Email
import com.loopers.domain.user.LoginId
import com.loopers.domain.user.User
import com.loopers.domain.user.UserRepository
import com.loopers.support.common.PageQuery
import com.loopers.support.common.SortOrder
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate
import java.time.ZonedDateTime

@SpringBootTest
class AdminCouponFacadeIntegrationTest @Autowired constructor(
    private val adminCouponFacade: AdminCouponFacade,
    private val couponRepository: CouponRepository,
    private val issuedCouponRepository: IssuedCouponRepository,
    private val userRepository: UserRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createCoupon(
        name: String = "신규가입 할인",
        discount: Discount = Discount(DiscountType.FIXED_AMOUNT, 5000L),
        totalQuantity: Int = 100,
        expiresAt: ZonedDateTime = ZonedDateTime.now().plusDays(30),
    ): Coupon {
        return couponRepository.save(
            Coupon(
                name = name,
                discount = discount,
                quantity = CouponQuantity(totalQuantity, 0),
                expiresAt = expiresAt,
            ),
        )
    }

    private fun createUser(
        loginId: String = "testuser",
        password: String = "Test1234!@",
        name: String = "홍길동",
        email: String = "test@example.com",
        birthday: LocalDate = LocalDate.of(1990, 1, 15),
    ): User {
        return userRepository.save(
            User(
                loginId = LoginId.of(loginId),
                password = password,
                name = name,
                birthday = birthday,
                email = Email.of(email),
            ),
        )
    }

    private fun createIssuedCoupon(couponId: Long, userId: Long): IssuedCoupon {
        return issuedCouponRepository.save(
            IssuedCoupon(couponId = couponId, userId = userId),
        )
    }

    @DisplayName("쿠폰 삭제할 때,")
    @Nested
    inner class DeleteCoupon {

        @DisplayName("DB에 저장된 쿠폰을 삭제하면, soft delete 되어 조회할 수 없다.")
        @Test
        fun softDeletesCoupon_whenCouponExistsInDb() {
            // arrange
            val saved = createCoupon(name = "삭제할 쿠폰")

            // act
            adminCouponFacade.deleteCoupon(saved.id)

            // assert
            val exception = assertThrows<CoreException> {
                adminCouponFacade.getCoupon(saved.id)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("존재하지 않는 쿠폰 ID로 삭제하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenCouponNotExistsInDb() {
            // act
            val exception = assertThrows<CoreException> {
                adminCouponFacade.deleteCoupon(999999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("이미 삭제된 쿠폰을 삭제하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenCouponAlreadyDeleted() {
            // arrange
            val saved = createCoupon(name = "삭제될 쿠폰")
            adminCouponFacade.deleteCoupon(saved.id)

            // act
            val exception = assertThrows<CoreException> {
                adminCouponFacade.deleteCoupon(saved.id)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("쿠폰 발급 내역을 조회할 때,")
    @Nested
    inner class GetCouponIssues {

        @DisplayName("발급 내역이 있으면, 사용자 정보와 발급 상태를 포함한 페이징 결과를 반환한다.")
        @Test
        fun returnsCouponIssuesWithUserInfo_whenIssuedCouponsExist() {
            // arrange
            val coupon = createCoupon()
            val user = createUser()
            createIssuedCoupon(coupon.id, user.id)
            val pageQuery = PageQuery(0, 20, SortOrder.UNSORTED)

            // act
            val result = adminCouponFacade.getCouponIssues(coupon.id, pageQuery)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(1) },
                { assertThat(result.content[0].userId).isEqualTo(user.id) },
                { assertThat(result.content[0].userName).isEqualTo(user.name) },
                { assertThat(result.content[0].status).isEqualTo(IssuedCouponStatus.AVAILABLE) },
                { assertThat(result.content[0].issuedAt).isNotNull() },
                { assertThat(result.content[0].usedAt).isNull() },
                { assertThat(result.totalElements).isEqualTo(1L) },
            )
        }

        @DisplayName("발급 내역이 없으면, 빈 페이지를 반환한다.")
        @Test
        fun returnsEmptyPage_whenNoIssuedCouponsExist() {
            // arrange
            val coupon = createCoupon()
            val pageQuery = PageQuery(0, 20, SortOrder.UNSORTED)

            // act
            val result = adminCouponFacade.getCouponIssues(coupon.id, pageQuery)

            // assert
            assertAll(
                { assertThat(result.content).isEmpty() },
                { assertThat(result.totalElements).isEqualTo(0L) },
            )
        }

        @DisplayName("존재하지 않는 쿠폰 ID이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenCouponNotExists() {
            // arrange
            val pageQuery = PageQuery(0, 20, SortOrder.UNSORTED)

            // act
            val exception = assertThrows<CoreException> {
                adminCouponFacade.getCouponIssues(999999L, pageQuery)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
