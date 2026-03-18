package com.loopers.infrastructure.id

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class SnowflakeIdGenerator(
    @Value("\${snowflake.machine-id:1}") private val machineId: Long,
) {
    companion object {
        private const val EPOCH = 1735689600000L
        private const val MACHINE_ID_BITS = 10L
        private const val SEQUENCE_BITS = 12L
        private const val MAX_MACHINE_ID = (1L shl MACHINE_ID_BITS.toInt()) - 1
        private const val MAX_SEQUENCE = (1L shl SEQUENCE_BITS.toInt()) - 1
        private const val MACHINE_ID_SHIFT = SEQUENCE_BITS
        private const val TIMESTAMP_SHIFT = SEQUENCE_BITS + MACHINE_ID_BITS
    }

    private var sequence = 0L
    private var lastTimestamp = -1L

    init {
        require(machineId in 0..MAX_MACHINE_ID) {
            "machineId는 0~$MAX_MACHINE_ID 범위여야 합니다."
        }
    }

    @Synchronized
    fun generate(): Long {
        var timestamp = System.currentTimeMillis()

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) and MAX_SEQUENCE
            if (sequence == 0L) {
                timestamp = waitNextMillis(lastTimestamp)
            }
        } else {
            sequence = 0L
        }

        lastTimestamp = timestamp

        return ((timestamp - EPOCH) shl TIMESTAMP_SHIFT.toInt()) or
            (machineId shl MACHINE_ID_SHIFT.toInt()) or
            sequence
    }

    private fun waitNextMillis(lastTimestamp: Long): Long {
        var timestamp = System.currentTimeMillis()
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis()
        }
        return timestamp
    }
}
