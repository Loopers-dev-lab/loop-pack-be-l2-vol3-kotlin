package com.loopers.domain.point.entity

import com.loopers.domain.BaseEntity
import com.loopers.domain.point.vo.Point
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "user_points")
class UserPoint(
    refUserId: Long,
    balance: Long = 0,
) : BaseEntity() {

    @Column(name = "ref_user_id", nullable = false, unique = true)
    var refUserId: Long = refUserId
        protected set

    @Column(name = "balance", nullable = false)
    var balance: Long = balance
        protected set

    init {
        guard()
    }

    override fun guard() {
        Point(balance)
    }

    fun charge(amount: Long) {
        val newBalance = Point(balance).plus(Point(amount)).value
        if (newBalance > MAX_BALANCE) {
            throw CoreException(ErrorType.BAD_REQUEST, "충전 후 잔액이 최대 한도(${MAX_BALANCE}포인트)를 초과합니다.")
        }
        this.balance = newBalance
    }

    companion object {
        const val MAX_BALANCE = 10_000_000L
    }

    fun use(amount: Long) {
        if (amount <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "사용 포인트는 0보다 커야 합니다.")
        }
        if (!canAfford(amount)) {
            throw CoreException(
                ErrorType.BAD_REQUEST,
                "포인트가 부족합니다. 필요: $amount, 현재: $balance",
            )
        }
        this.balance = Point(balance).minus(Point(amount)).value
    }

    fun canAfford(amount: Long): Boolean {
        return balance >= amount
    }
}
