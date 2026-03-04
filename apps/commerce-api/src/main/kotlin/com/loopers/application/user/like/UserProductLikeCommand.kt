package com.loopers.application.user.like

class UserProductLikeCommand {
    data class Register(val userId: Long, val productId: Long)

    data class Cancel(val userId: Long, val productId: Long)
}
