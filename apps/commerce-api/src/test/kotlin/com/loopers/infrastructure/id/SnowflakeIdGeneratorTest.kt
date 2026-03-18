package com.loopers.infrastructure.id

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class SnowflakeIdGeneratorTest {

    @DisplayName("ID를 생성할 때,")
    @Nested
    inner class Generate {

        @DisplayName("양수 Long 값이 생성된다.")
        @Test
        fun generatesPositiveLong() {
            // arrange
            val generator = SnowflakeIdGenerator(machineId = 1)

            // act
            val id = generator.generate()

            // assert
            assertThat(id).isGreaterThan(0L)
        }

        @DisplayName("연속 생성 시 항상 다른 값이 생성된다.")
        @Test
        fun generatesUniqueIds() {
            // arrange
            val generator = SnowflakeIdGenerator(machineId = 1)

            // act
            val ids = (1..1000).map { generator.generate() }.toSet()

            // assert
            assertThat(ids).hasSize(1000)
        }

        @DisplayName("생성된 ID는 시간순으로 증가한다.")
        @Test
        fun generatesIdsInIncreasingOrder() {
            // arrange
            val generator = SnowflakeIdGenerator(machineId = 1)

            // act
            val ids = (1..100).map { generator.generate() }

            // assert
            assertThat(ids).isSorted()
        }

        @DisplayName("PG orderId 6자 이상 조건을 충족한다.")
        @Test
        fun satisfiesPgMinLengthRequirement() {
            // arrange
            val generator = SnowflakeIdGenerator(machineId = 1)

            // act
            val id = generator.generate()
            val idString = id.toString()

            // assert
            assertThat(idString.length).isGreaterThanOrEqualTo(6)
        }
    }

    @DisplayName("동시성 환경에서,")
    @Nested
    inner class Concurrency {

        @DisplayName("멀티 스레드에서 동시 생성해도 중복이 없다.")
        @Test
        fun generatesUniqueIdsUnderConcurrency() {
            // arrange
            val generator = SnowflakeIdGenerator(machineId = 1)
            val threadCount = 10
            val idsPerThread = 1000
            val ids = ConcurrentHashMap.newKeySet<Long>()
            val latch = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(threadCount)

            // act
            repeat(threadCount) {
                executor.submit {
                    latch.await()
                    repeat(idsPerThread) {
                        ids.add(generator.generate())
                    }
                }
            }
            latch.countDown()
            executor.shutdown()
            executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)

            // assert
            assertThat(ids).hasSize(threadCount * idsPerThread)
        }
    }

    @DisplayName("머신 ID를 설정할 때,")
    @Nested
    inner class MachineId {

        @DisplayName("다른 머신 ID로 같은 시점에 생성해도 다른 ID가 나온다.")
        @Test
        fun generatesDifferentIdsForDifferentMachines() {
            // arrange
            val generator1 = SnowflakeIdGenerator(machineId = 1)
            val generator2 = SnowflakeIdGenerator(machineId = 2)

            // act
            val id1 = generator1.generate()
            val id2 = generator2.generate()

            // assert
            assertThat(id1).isNotEqualTo(id2)
        }

        @DisplayName("범위 밖의 머신 ID(음수)이면, 예외가 발생한다.")
        @Test
        fun throwsException_whenMachineIdIsNegative() {
            // act & assert
            assertThrows<IllegalArgumentException> {
                SnowflakeIdGenerator(machineId = -1)
            }
        }

        @DisplayName("범위 밖의 머신 ID(1024 이상)이면, 예외가 발생한다.")
        @Test
        fun throwsException_whenMachineIdExceedsMax() {
            // act & assert
            assertThrows<IllegalArgumentException> {
                SnowflakeIdGenerator(machineId = 1024)
            }
        }
    }
}
