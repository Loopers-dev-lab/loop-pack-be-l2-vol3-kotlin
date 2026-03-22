package com.loopers.domain

import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong

/**
 * Snowflake ID Generator
 * 분산 시스템에서 고유한 ID를 생성합니다.
 *
 * 구조:
 * - Timestamp (41 bits): milliseconds since epoch
 * - DataCenter ID (5 bits): 0-31
 * - Worker ID (5 bits): 0-31
 * - Sequence (12 bits): 0-4095
 */
@Component
class SnowflakeIdGenerator(
    private val datacenterId: Long = 1,
    private val workerId: Long = 1,
) {
    private val epoch = 1609459200000L // 2021-01-01 00:00:00 UTC
    private val sequenceBits = 12L
    private val workerIdBits = 5L
    private val dataCenterIdBits = 5L
    private val maxSequence = (1L shl sequenceBits.toInt()) - 1
    private val workerIdShift = sequenceBits
    private val dataCenterIdShift = sequenceBits + workerIdBits
    private val timestampShift = sequenceBits + workerIdBits + dataCenterIdBits

    private var lastTimestamp = -1L
    private val sequence = AtomicLong(0)

    @Synchronized
    fun nextId(): Long {
        var timestamp = currentTimeMillis()

        if (timestamp == lastTimestamp) {
            val seq = sequence.incrementAndGet()
            if (seq > maxSequence) {
                timestamp = tilNextMillis()
                sequence.set(0)
            }
        } else {
            sequence.set(0)
        }

        lastTimestamp = timestamp

        return (
            (timestamp - epoch) shl timestampShift.toInt()
            ) or (
            (datacenterId and ((1L shl dataCenterIdBits.toInt()) - 1)) shl dataCenterIdShift.toInt()
            ) or (
            (workerId and ((1L shl workerIdBits.toInt()) - 1)) shl workerIdShift.toInt()
            ) or (
            sequence.get() and maxSequence
            )
    }

    private fun currentTimeMillis(): Long = System.currentTimeMillis()

    private fun tilNextMillis(): Long {
        var timestamp = currentTimeMillis()
        while (timestamp <= lastTimestamp) {
            timestamp = currentTimeMillis()
        }
        return timestamp
    }
}
