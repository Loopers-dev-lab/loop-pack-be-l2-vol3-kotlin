package com.loopers.domain.coupon

import com.loopers.application.coupon.CouponFacade
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
import java.time.ZonedDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@SpringBootTest
class CouponServiceIntegrationTest @Autowired constructor(
    private val couponService: CouponService,
    private val couponFacade: CouponFacade,
    private val couponRepository: CouponRepository,
    private val issuedCouponRepository: IssuedCouponRepository,
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

    @DisplayName("쿠폰을 생성할 때,")
    @Nested
    inner class CreateCoupon {

        @DisplayName("유효한 값이 주어지면, DB에 저장되고 쿠폰 정보가 반환된다.")
        @Test
        fun createsCoupon_whenValidValuesProvided() {
            // arrange
            val name = "신규가입 10% 할인"
            val discount = Discount(DiscountType.PERCENTAGE, 10L)
            val quantity = CouponQuantity(100, 0)
            val expiresAt = ZonedDateTime.now().plusDays(30)

            // act
            val coupon = couponService.create(
                name = name,
                discount = discount,
                quantity = quantity,
                expiresAt = expiresAt,
            )

            // assert
            val found = couponService.findCouponById(coupon.id)
            assertAll(
                { assertThat(found.name).isEqualTo(name) },
                { assertThat(found.discount.type).isEqualTo(DiscountType.PERCENTAGE) },
                { assertThat(found.discount.value).isEqualTo(10L) },
                { assertThat(found.quantity.total).isEqualTo(100) },
                { assertThat(found.quantity.issued).isEqualTo(0) },
                { assertThat(found.expiresAt).isNotNull() },
                { assertThat(found.createdAt).isNotNull() },
            )
        }
    }

    @DisplayName("쿠폰을 발급할 때,")
    @Nested
    inner class IssueCoupon {

        @DisplayName("유효한 쿠폰이면, 발급에 성공한다.")
        @Test
        fun issuesCoupon_whenCouponIsValid() {
            // arrange
            val coupon = createCoupon()
            val userId = 1L

            // act
            couponFacade.issue(couponId = coupon.id, userId = userId)

            // assert
            val issuedCoupons = issuedCouponRepository.findByUserId(userId)
            assertThat(issuedCoupons).hasSize(1)
            assertThat(issuedCoupons[0].couponId).isEqualTo(coupon.id)
        }

        @DisplayName("이미 발급받은 쿠폰이면, CONFLICT 예외가 발생한다.")
        @Test
        fun throwsConflict_whenAlreadyIssued() {
            // arrange
            val coupon = createCoupon()
            val userId = 1L
            couponFacade.issue(couponId = coupon.id, userId = userId)

            // act
            val exception = assertThrows<CoreException> {
                couponFacade.issue(couponId = coupon.id, userId = userId)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.CONFLICT)
        }

        @DisplayName("만료된 쿠폰이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenCouponIsExpired() {
            // arrange
            val coupon = createCoupon(expiresAt = ZonedDateTime.now().minusDays(1))
            val userId = 1L

            // act
            val exception = assertThrows<CoreException> {
                couponFacade.issue(couponId = coupon.id, userId = userId)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("발급 수량이 소진된 쿠폰이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenCouponIsExhausted() {
            // arrange
            val coupon = createCoupon(totalQuantity = 1)
            couponFacade.issue(couponId = coupon.id, userId = 1L)

            // act
            val exception = assertThrows<CoreException> {
                couponFacade.issue(couponId = coupon.id, userId = 2L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("존재하지 않는 쿠폰이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenCouponNotExists() {
            // act
            val exception = assertThrows<CoreException> {
                couponFacade.issue(couponId = 999999L, userId = 1L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("사용자의 쿠폰 목록을 조회할 때,")
    @Nested
    inner class GetMyCoupons {

        @DisplayName("발급받은 쿠폰이 있으면, 상태와 함께 반환된다.")
        @Test
        fun returnsCouponsWithStatus_whenUserHasIssuedCoupons() {
            // arrange
            val coupon = createCoupon()
            val userId = 1L
            couponFacade.issue(couponId = coupon.id, userId = userId)

            // act
            val issuedCoupons = issuedCouponRepository.findByUserId(userId)
            val savedCoupon = couponRepository.findById(coupon.id)!!

            // assert
            assertThat(issuedCoupons).hasSize(1)
            assertThat(issuedCoupons[0].status(savedCoupon.expiresAt))
                .isEqualTo(IssuedCouponStatus.AVAILABLE)
        }

        @DisplayName("발급받은 쿠폰이 없으면, 빈 목록이 반환된다.")
        @Test
        fun returnsEmptyList_whenUserHasNoCoupons() {
            // act
            val issuedCoupons = issuedCouponRepository.findByUserId(1L)

            // assert
            assertThat(issuedCoupons).isEmpty()
        }

        @DisplayName("만료된 쿠폰이면, EXPIRED 상태로 반환된다.")
        @Test
        fun returnsExpiredStatus_whenCouponIsExpired() {
            // arrange
            val coupon = createCoupon(expiresAt = ZonedDateTime.now().minusDays(1))
            val userId = 1L
            issuedCouponRepository.save(IssuedCoupon(couponId = coupon.id, userId = userId))

            // act
            val issuedCoupons = issuedCouponRepository.findByUserId(userId)
            val savedCoupon = couponRepository.findById(coupon.id)!!

            // assert
            assertThat(issuedCoupons).hasSize(1)
            assertThat(issuedCoupons[0].status(savedCoupon.expiresAt))
                .isEqualTo(IssuedCouponStatus.EXPIRED)
        }
    }

    @DisplayName("쿠폰을 ID로 조회할 때,")
    @Nested
    inner class FindCouponById {

        @DisplayName("존재하는 쿠폰 ID이면, 쿠폰을 반환한다.")
        @Test
        fun returnsCoupon_whenCouponExistsInDb() {
            // arrange
            val coupon = createCoupon(
                name = "신규가입 할인",
                discount = Discount(DiscountType.FIXED_AMOUNT, 5000L),
            )

            // act
            val result = couponService.findCouponById(coupon.id)

            // assert
            assertAll(
                { assertThat(result.id).isEqualTo(coupon.id) },
                { assertThat(result.name).isEqualTo("신규가입 할인") },
                { assertThat(result.discount.type).isEqualTo(DiscountType.FIXED_AMOUNT) },
                { assertThat(result.discount.value).isEqualTo(5000L) },
                { assertThat(result.quantity.total).isEqualTo(100) },
                { assertThat(result.expiresAt).isNotNull() },
                { assertThat(result.createdAt).isNotNull() },
            )
        }

        @DisplayName("존재하지 않는 쿠폰 ID이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenCouponNotExistsInDb() {
            // act
            val exception = assertThrows<CoreException> {
                couponService.findCouponById(999999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("삭제된 쿠폰 ID이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenCouponIsDeleted() {
            // arrange
            val coupon = createCoupon(name = "삭제될 쿠폰")
            coupon.delete()
            couponRepository.save(coupon)

            // act
            val exception = assertThrows<CoreException> {
                couponService.findCouponById(coupon.id)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("쿠폰 목록을 페이징 조회할 때,")
    @Nested
    inner class FindAll {

        @DisplayName("DB에 저장된 쿠폰을 페이징으로 조회하면, 해당 페이지의 쿠폰을 반환한다.")
        @Test
        fun returnsPagedCoupons_whenCouponsExistInDb() {
            // arrange
            createCoupon(name = "신규가입 할인")
            createCoupon(name = "여름 할인", discount = Discount(DiscountType.PERCENTAGE, 15L))
            createCoupon(name = "VIP 할인")
            val pageQuery = PageQuery(0, 2, SortOrder.UNSORTED)

            // act
            val result = couponService.findAll(pageQuery)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(2) },
                { assertThat(result.totalElements).isEqualTo(3) },
                { assertThat(result.totalPages).isEqualTo(2) },
            )
        }

        @DisplayName("쿠폰이 없으면, 빈 페이지를 반환한다.")
        @Test
        fun returnsEmptyPage_whenNoCouponsExistInDb() {
            // arrange
            val pageQuery = PageQuery(0, 20, SortOrder.UNSORTED)

            // act
            val result = couponService.findAll(pageQuery)

            // assert
            assertAll(
                { assertThat(result.content).isEmpty() },
                { assertThat(result.totalElements).isEqualTo(0) },
            )
        }

        @DisplayName("삭제된 쿠폰은 목록에 포함되지 않는다.")
        @Test
        fun excludesDeletedCoupons() {
            // arrange
            createCoupon(name = "활성 쿠폰")
            val deletedCoupon = createCoupon(name = "삭제될 쿠폰")
            deletedCoupon.delete()
            couponRepository.save(deletedCoupon)
            val pageQuery = PageQuery(0, 20, SortOrder.UNSORTED)

            // act
            val result = couponService.findAll(pageQuery)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(1) },
                { assertThat(result.totalElements).isEqualTo(1) },
                { assertThat(result.content[0].name).isEqualTo("활성 쿠폰") },
            )
        }

        @DisplayName("응답에 쿠폰 타입, 할인값, 만료일, 생성일이 포함된다.")
        @Test
        fun includesCouponDetails() {
            // arrange
            createCoupon(
                name = "고정액 할인",
                discount = Discount(DiscountType.FIXED_AMOUNT, 3000L),
            )
            val pageQuery = PageQuery(0, 20, SortOrder.UNSORTED)

            // act
            val result = couponService.findAll(pageQuery)

            // assert
            val found = result.content[0]
            assertAll(
                { assertThat(found.discount.type).isEqualTo(DiscountType.FIXED_AMOUNT) },
                { assertThat(found.discount.value).isEqualTo(3000L) },
                { assertThat(found.expiresAt).isNotNull() },
                { assertThat(found.createdAt).isNotNull() },
            )
        }
    }

    @DisplayName("동시에 여러 사용자가 같은 쿠폰 발급을 요청하면,")
    @Nested
    inner class ConcurrentIssueCoupon {

        @DisplayName("정확한 수량만큼만 발급된다.")
        @Test
        fun issuesExactQuantity_whenConcurrentRequests() {
            // arrange
            val coupon = createCoupon(totalQuantity = 10)
            val threadCount = 20
            val executorService = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)

            // act
            (1..threadCount).forEach { userId ->
                executorService.submit {
                    try {
                        couponFacade.issue(couponId = coupon.id, userId = userId.toLong())
                    } catch (_: CoreException) {
                        // 수량 초과 시 예외 발생 예상
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
            executorService.shutdown()

            // assert
            val issuedCoupons = issuedCouponRepository.findByCouponId(coupon.id)
            assertThat(issuedCoupons).hasSize(10)
        }
    }
}
