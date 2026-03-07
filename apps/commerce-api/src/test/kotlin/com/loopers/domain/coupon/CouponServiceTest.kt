package com.loopers.domain.coupon

import com.loopers.domain.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageRequest
import java.time.ZonedDateTime

class CouponServiceTest {

    private lateinit var couponService: CouponService
    private lateinit var couponRepository: FakeCouponRepository
    private lateinit var couponIssueRepository: FakeCouponIssueRepository

    @BeforeEach
    fun setUp() {
        couponRepository = FakeCouponRepository()
        couponIssueRepository = FakeCouponIssueRepository()
        couponService = CouponService(couponRepository, couponIssueRepository)
    }

    private fun createCouponCommand(
        name: String = "신규가입 쿠폰",
        type: CouponType = CouponType.FIXED,
        value: Long = 5000,
        expiredAt: ZonedDateTime = ZonedDateTime.now().plusDays(30),
    ) = CreateCouponCommand(
        name = name,
        type = type,
        value = value,
        expiredAt = expiredAt,
    )

    // ──────────────────────────────────────────
    // 쿠폰 템플릿 (Admin)
    // ──────────────────────────────────────────

    @Nested
    inner class CreateCoupon {

        @Test
        @DisplayName("올바른 정보로 쿠폰을 생성하면 성공한다")
        fun success() {
            // arrange
            val command = createCouponCommand()

            // act
            val coupon = couponService.createCoupon(command)

            // assert
            assertAll(
                { assertThat(coupon.id).isGreaterThan(0) },
                { assertThat(coupon.name).isEqualTo("신규가입 쿠폰") },
                { assertThat(coupon.type).isEqualTo(CouponType.FIXED) },
                { assertThat(coupon.value).isEqualTo(5000) },
            )
        }

        @Test
        @DisplayName("쿠폰명이 빈칸이면 BAD_REQUEST 예외가 발생한다")
        fun nameBlankThrowsBadRequest() {
            val command = createCouponCommand(name = "  ")

            val result = assertThrows<CoreException> {
                couponService.createCoupon(command)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @Nested
    inner class UpdateCoupon {

        @Test
        @DisplayName("존재하는 쿠폰을 수정하면 성공한다")
        fun success() {
            // arrange
            val created = couponService.createCoupon(createCouponCommand())
            val command = UpdateCouponCommand(
                name = "봄맞이 쿠폰",
                type = CouponType.RATE,
                value = 15,
                expiredAt = ZonedDateTime.now().plusDays(60),
            )

            // act
            val updated = couponService.updateCoupon(created.id, command)

            // assert
            assertAll(
                { assertThat(updated.name).isEqualTo("봄맞이 쿠폰") },
                { assertThat(updated.type).isEqualTo(CouponType.RATE) },
                { assertThat(updated.value).isEqualTo(15) },
            )
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰을 수정하면 NOT_FOUND 예외가 발생한다")
        fun notFoundThrowsNotFound() {
            val command = UpdateCouponCommand(
                name = "쿠폰",
                type = CouponType.FIXED,
                value = 1000,
                expiredAt = ZonedDateTime.now().plusDays(30),
            )

            val result = assertThrows<CoreException> {
                couponService.updateCoupon(999L, command)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @Nested
    inner class DeleteCoupon {

        @Test
        @DisplayName("존재하는 쿠폰을 삭제하면 조회되지 않는다")
        fun success() {
            // arrange
            val created = couponService.createCoupon(createCouponCommand())

            // act
            couponService.deleteCoupon(created.id)

            // assert
            val result = assertThrows<CoreException> {
                couponService.findCouponById(created.id)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰을 삭제하면 NOT_FOUND 예외가 발생한다")
        fun notFoundThrowsNotFound() {
            val result = assertThrows<CoreException> {
                couponService.deleteCoupon(999L)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @Nested
    inner class FindCouponById {

        @Test
        @DisplayName("존재하는 쿠폰을 조회하면 성공한다")
        fun success() {
            // arrange
            val created = couponService.createCoupon(createCouponCommand())

            // act
            val found = couponService.findCouponById(created.id)

            // assert
            assertThat(found.name).isEqualTo("신규가입 쿠폰")
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰을 조회하면 NOT_FOUND 예외가 발생한다")
        fun notFoundThrowsNotFound() {
            val result = assertThrows<CoreException> {
                couponService.findCouponById(999L)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @Nested
    inner class FindAllCoupons {

        @Test
        @DisplayName("쿠폰 목록을 페이징 조회한다")
        fun success() {
            // arrange
            couponService.createCoupon(createCouponCommand(name = "쿠폰A"))
            couponService.createCoupon(createCouponCommand(name = "쿠폰B"))
            couponService.createCoupon(createCouponCommand(name = "쿠폰C"))

            // act
            val page = couponService.findAllCoupons(PageRequest.of(0, 2))

            // assert
            assertAll(
                { assertThat(page.content).hasSize(2) },
                { assertThat(page.totalElements).isEqualTo(3) },
                { assertThat(page.totalPages).isEqualTo(2) },
            )
        }

        @Test
        @DisplayName("삭제된 쿠폰은 목록에서 제외된다")
        fun excludeDeleted() {
            // arrange
            val created = couponService.createCoupon(createCouponCommand(name = "쿠폰A"))
            couponService.createCoupon(createCouponCommand(name = "쿠폰B"))
            couponService.deleteCoupon(created.id)

            // act
            val page = couponService.findAllCoupons(PageRequest.of(0, 20))

            // assert
            assertThat(page.content).hasSize(1)
        }
    }

    // ──────────────────────────────────────────
    // 쿠폰 발급 (User)
    // ──────────────────────────────────────────

    @Nested
    inner class IssueCoupon {

        @Test
        @DisplayName("유효한 쿠폰을 발급받으면 AVAILABLE 상태로 생성된다")
        fun success() {
            // arrange
            val coupon = couponService.createCoupon(createCouponCommand())

            // act
            val issue = couponService.issueCoupon(coupon.id, userId = 1L)

            // assert
            assertAll(
                { assertThat(issue.id).isGreaterThan(0) },
                { assertThat(issue.couponId).isEqualTo(coupon.id) },
                { assertThat(issue.userId).isEqualTo(1L) },
                { assertThat(issue.status).isEqualTo(CouponIssueStatus.AVAILABLE) },
            )
        }

        @Test
        @DisplayName("만료된 쿠폰을 발급받으려 하면 BAD_REQUEST 예외가 발생한다")
        fun expiredCouponThrowsBadRequest() {
            // arrange
            val coupon = couponService.createCoupon(
                createCouponCommand(expiredAt = ZonedDateTime.now().minusDays(1)),
            )

            // act
            val result = assertThrows<CoreException> {
                couponService.issueCoupon(coupon.id, userId = 1L)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("이미 발급받은 쿠폰을 다시 발급받으려 하면 CONFLICT 예외가 발생한다")
        fun duplicateIssueThrowsConflict() {
            // arrange
            val coupon = couponService.createCoupon(createCouponCommand())
            couponService.issueCoupon(coupon.id, userId = 1L)

            // act
            val result = assertThrows<CoreException> {
                couponService.issueCoupon(coupon.id, userId = 1L)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.CONFLICT)
        }

        @Test
        @DisplayName("다른 유저는 같은 쿠폰을 발급받을 수 있다")
        fun differentUserCanIssue() {
            // arrange
            val coupon = couponService.createCoupon(createCouponCommand())
            couponService.issueCoupon(coupon.id, userId = 1L)

            // act
            val issue = couponService.issueCoupon(coupon.id, userId = 2L)

            // assert
            assertThat(issue.userId).isEqualTo(2L)
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰을 발급받으려 하면 NOT_FOUND 예외가 발생한다")
        fun notFoundThrowsNotFound() {
            val result = assertThrows<CoreException> {
                couponService.issueCoupon(999L, userId = 1L)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    // ──────────────────────────────────────────
    // 주문 시 쿠폰 사용
    // ──────────────────────────────────────────

    @Nested
    inner class UseCouponForOrder {

        @Test
        @DisplayName("유효한 쿠폰을 주문에 사용하면 할인 금액이 반환된다")
        fun success() {
            // arrange
            val coupon = couponService.createCoupon(createCouponCommand(value = 5000))
            val issue = couponService.issueCoupon(coupon.id, userId = 1L)

            // act
            val result = couponService.useCouponForOrder(issue.id, userId = 1L, orderAmount = Money(30000))

            // assert
            assertAll(
                { assertThat(result.couponIssueId).isEqualTo(issue.id) },
                { assertThat(result.discountAmount).isEqualTo(Money(5000)) },
            )
        }

        @Test
        @DisplayName("타인의 쿠폰을 사용하면 FORBIDDEN 예외가 발생한다")
        fun otherUserThrowsForbidden() {
            // arrange
            val coupon = couponService.createCoupon(createCouponCommand())
            val issue = couponService.issueCoupon(coupon.id, userId = 1L)

            // act
            val result = assertThrows<CoreException> {
                couponService.useCouponForOrder(issue.id, userId = 2L, orderAmount = Money(30000))
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.FORBIDDEN)
        }

        @Test
        @DisplayName("이미 사용된 쿠폰을 다시 사용하면 BAD_REQUEST 예외가 발생한다")
        fun alreadyUsedThrowsBadRequest() {
            // arrange
            val coupon = couponService.createCoupon(createCouponCommand())
            val issue = couponService.issueCoupon(coupon.id, userId = 1L)
            couponService.useCouponForOrder(issue.id, userId = 1L, orderAmount = Money(30000))

            // act
            val result = assertThrows<CoreException> {
                couponService.useCouponForOrder(issue.id, userId = 1L, orderAmount = Money(30000))
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("만료된 쿠폰을 사용하면 BAD_REQUEST 예외가 발생한다")
        fun expiredCouponThrowsBadRequest() {
            // arrange
            val coupon = couponService.createCoupon(
                createCouponCommand(expiredAt = ZonedDateTime.now().plusDays(1)),
            )
            val issue = couponService.issueCoupon(coupon.id, userId = 1L)

            // 쿠폰 만료 시간을 과거로 변경
            coupon.update(
                name = coupon.name,
                type = coupon.type,
                value = coupon.value,
                expiredAt = ZonedDateTime.now().minusDays(1),
            )

            // act
            val result = assertThrows<CoreException> {
                couponService.useCouponForOrder(issue.id, userId = 1L, orderAmount = Money(30000))
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("존재하지 않는 발급 쿠폰을 사용하면 NOT_FOUND 예외가 발생한다")
        fun notFoundThrowsNotFound() {
            val result = assertThrows<CoreException> {
                couponService.useCouponForOrder(999L, userId = 1L, orderAmount = Money(30000))
            }
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("정률 쿠폰으로 주문 금액의 비율만큼 할인받는다")
        fun rateCouponDiscount() {
            // arrange
            val coupon = couponService.createCoupon(
                createCouponCommand(type = CouponType.RATE, value = 10),
            )
            val issue = couponService.issueCoupon(coupon.id, userId = 1L)

            // act
            val result = couponService.useCouponForOrder(issue.id, userId = 1L, orderAmount = Money(50000))

            // assert
            assertThat(result.discountAmount).isEqualTo(Money(5000))
        }
    }

    @Nested
    inner class FindAllByUserId {

        @Test
        @DisplayName("유저의 발급 쿠폰 목록을 조회한다")
        fun success() {
            // arrange
            val couponA = couponService.createCoupon(createCouponCommand(name = "쿠폰A"))
            val couponB = couponService.createCoupon(createCouponCommand(name = "쿠폰B"))
            couponService.issueCoupon(couponA.id, userId = 1L)
            couponService.issueCoupon(couponB.id, userId = 1L)
            couponService.issueCoupon(couponA.id, userId = 2L)

            // act
            val result = couponService.findAllByUserId(userId = 1L)

            // assert
            assertThat(result).hasSize(2)
        }
    }

    @Nested
    inner class FindCouponIssueById {

        @Test
        @DisplayName("존재하는 발급 쿠폰을 조회하면 성공한다")
        fun success() {
            // arrange
            val coupon = couponService.createCoupon(createCouponCommand())
            val issue = couponService.issueCoupon(coupon.id, userId = 1L)

            // act
            val found = couponService.findCouponIssueById(issue.id)

            // assert
            assertThat(found.couponId).isEqualTo(coupon.id)
        }

        @Test
        @DisplayName("존재하지 않는 발급 쿠폰을 조회하면 NOT_FOUND 예외가 발생한다")
        fun notFoundThrowsNotFound() {
            val result = assertThrows<CoreException> {
                couponService.findCouponIssueById(999L)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
