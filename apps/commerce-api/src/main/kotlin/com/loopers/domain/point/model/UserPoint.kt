package com.loopers.domain.point.model

import com.loopers.domain.point.vo.Point
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

class UserPoint(
    val id: Long = 0,
    val refUserId: Long,
    balance: Long = 0,
) {

    var balance: Long = balance
        private set

    init {
        Point(balance)
    }

    fun charge(amount: Point) {
        if (amount.value == 0L) {
            throw CoreException(ErrorType.BAD_REQUEST, "충전 금액은 0보다 커야 합니다.")
        }
        val newBalance = Point(balance).plus(amount).value
        if (newBalance > MAX_BALANCE) {
            throw CoreException(ErrorType.BAD_REQUEST, "충전 후 잔액이 최대 한도(${MAX_BALANCE}포인트)를 초과합니다.")
        }
        this.balance = newBalance
    }

    companion object {
        const val MAX_BALANCE = 10_000_000L
    }

    fun use(amount: Point) {
        if (amount.value == 0L) {
            throw CoreException(ErrorType.BAD_REQUEST, "사용 포인트는 0보다 커야 합니다.")
        }
        if (!canAfford(amount)) {
            throw CoreException(
                ErrorType.BAD_REQUEST,
                "포인트가 부족합니다. 필요: ${amount.value}, 현재: $balance",
            )
        }
        this.balance = Point(balance).minus(amount).value
    }

    fun canAfford(amount: Point): Boolean {
        return Point(balance).isGreaterThanOrEqual(amount)
    }
}
