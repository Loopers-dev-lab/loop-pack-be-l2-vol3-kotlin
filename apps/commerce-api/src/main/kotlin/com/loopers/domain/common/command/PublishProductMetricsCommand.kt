package com.loopers.domain.common.command

data class PublishProductMetricsCommand(
    val memberId: Long,
    val actionType: String,
    val targetType: String,
    val targetId: Long,
)
