package com.loopers.application.api.coupon

import com.loopers.domain.coupon.CouponTemplate
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.coupon.strategy.LimitedCouponIssuanceStrategy
import com.loopers.domain.coupon.strategy.NormalCouponIssuanceStrategy
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.ZonedDateTime

@DisplayName("CouponIssuanceStrategySelector")
class CouponIssuanceStrategySelectorTest {

    private val selector = CouponIssuanceStrategySelector()

    @DisplayName("totalCount가 있는 쿠폰 → LimitedCouponIssuanceStrategy 반환")
    @Test
    fun select_limitedCoupon_returnsLimitedStrategy() {
        // Arrange
        val template = CouponTemplate.createForTest(
            name = "선착순 100장",
            type = CouponType.FIXED,
            value = BigDecimal("1000"),
            minOrderAmount = BigDecimal("5000"),
            expiredAt = ZonedDateTime.now().plusDays(30),
            totalCount = 100,
        )

        // Act
        val strategy = selector.select(template)

        // Assert
        assertThat(strategy).isInstanceOf(LimitedCouponIssuanceStrategy::class.java)
        assertThat(strategy.getTopic()).isEqualTo("coupon-limited-events")
        assertThat(strategy.getPartitionKey(100L)).isEqualTo("limited:100")
    }

    @DisplayName("totalCount가 없는 쿠폰 → NormalCouponIssuanceStrategy 반환")
    @Test
    fun select_unlimitedCoupon_returnsNormalStrategy() {
        // Arrange
        val template = CouponTemplate.createForTest(
            name = "일반 쿠폰",
            type = CouponType.FIXED,
            value = BigDecimal("1000"),
            minOrderAmount = BigDecimal("5000"),
            expiredAt = ZonedDateTime.now().plusDays(30),
        )
        // totalCount = null (무제한)

        // Act
        val strategy = selector.select(template)

        // Assert
        assertThat(strategy).isInstanceOf(NormalCouponIssuanceStrategy::class.java)
        assertThat(strategy.getTopic()).isEqualTo("coupon-normal-events")
        assertThat(strategy.getPartitionKey(200L)).isEqualTo("normal:200")
    }

    @DisplayName("선착순 쿠폰 파티션 키 형식 검증")
    @Test
    fun limitedStrategy_partitionKeyFormat() {
        // Arrange
        val strategy = LimitedCouponIssuanceStrategy()

        // Act & Assert
        assertThat(strategy.getPartitionKey(100L)).startsWith("limited:")
        assertThat(strategy.getPartitionKey(100L)).endsWith("100")
        assertThat(strategy.getPartitionKey(999L)).isEqualTo("limited:999")
    }

    @DisplayName("일반 쿠폰 파티션 키 형식 검증")
    @Test
    fun normalStrategy_partitionKeyFormat() {
        // Arrange
        val strategy = NormalCouponIssuanceStrategy()

        // Act & Assert
        assertThat(strategy.getPartitionKey(100L)).startsWith("normal:")
        assertThat(strategy.getPartitionKey(100L)).endsWith("100")
        assertThat(strategy.getPartitionKey(999L)).isEqualTo("normal:999")
    }
}
