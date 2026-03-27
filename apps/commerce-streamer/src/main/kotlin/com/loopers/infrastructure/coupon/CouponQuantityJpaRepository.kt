package com.loopers.infrastructure.coupon

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository

/**
 * 쿠폰 수량 atomic increment 전용.
 *
 * Coupon 엔티티가 streamer에 없으므로 EntityManager native query를 직접 사용한다.
 * (JpaRepository 상속 시 엔티티 타입이 필요한데, Coupon 엔티티를 두면
 *  Hibernate DDL이 coupons 테이블을 streamer 기준으로 재생성하는 문제가 발생)
 *
 * WHERE 조건에서 issued_count < max_issue_count를 확인하므로,
 * 수량 초과 발급이 DB 레벨에서 방지된다.
 */
@Repository
class CouponQuantityJpaRepository(
    private val entityManager: EntityManager,
) {

    fun incrementIssuedCount(couponId: Long): Int {
        return entityManager.createNativeQuery(
            """
            UPDATE coupons
            SET issued_count = issued_count + 1, updated_at = NOW()
            WHERE id = :couponId
            AND deleted_at IS NULL
            AND (max_issue_count IS NULL OR issued_count < max_issue_count)
            """,
        ).setParameter("couponId", couponId)
            .executeUpdate()
    }
}
