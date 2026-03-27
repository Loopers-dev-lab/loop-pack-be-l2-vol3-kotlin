package com.loopers.domain.common.event

data class MemberCreatedEvent(
    val memberId: Long,
    val loginId: String,
)

data class MemberUpdatedEvent(
    val memberId: Long,
)

data class MemberPasswordChangedEvent(
    val memberId: Long,
    val loginId: String,
)
