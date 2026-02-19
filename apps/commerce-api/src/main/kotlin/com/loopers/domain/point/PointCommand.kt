package com.loopers.domain.point

sealed interface PointCommand {
    data class Charge(val amount: Long) : PointCommand
}
