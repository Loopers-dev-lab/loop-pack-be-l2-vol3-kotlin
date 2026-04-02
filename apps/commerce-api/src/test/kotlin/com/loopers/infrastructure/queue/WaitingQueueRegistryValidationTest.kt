package com.loopers.infrastructure.queue

import com.loopers.support.annotation.WaitingQueue
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.ApplicationContext
import org.springframework.web.bind.annotation.RestController

class WaitingQueueRegistryValidationTest {

    private lateinit var registry: WaitingQueueRegistry

    // TDD validation test

    @DisplayName("어노테이션 값 검증")
    @Nested
    inner class AnnotationValidationTest {

        @Test
        fun `throughput이 0 이하이면 예외를 발생시킨다`() {
            // arrange
            val applicationContext = mockk<ApplicationContext>()
            every { applicationContext.getBeansWithAnnotation(RestController::class.java) } returns mapOf(
                "testController" to InvalidThroughputController(),
            )
            registry = WaitingQueueRegistry(applicationContext)

            // act & assert
            assertThrows<IllegalArgumentException> {
                registry.afterSingletonsInstantiated()
            }
        }

        @Test
        fun `TTL이 0 이하이면 예외를 발생시킨다`() {
            // arrange
            val applicationContext = mockk<ApplicationContext>()
            every { applicationContext.getBeansWithAnnotation(RestController::class.java) } returns mapOf(
                "testController" to InvalidTTLController(),
            )
            registry = WaitingQueueRegistry(applicationContext)

            // act & assert
            assertThrows<IllegalArgumentException> {
                registry.afterSingletonsInstantiated()
            }
        }

        @Test
        fun `name이 blank이면 예외를 발생시킨다`() {
            // arrange
            val applicationContext = mockk<ApplicationContext>()
            every { applicationContext.getBeansWithAnnotation(RestController::class.java) } returns mapOf(
                "testController" to InvalidNameController(),
            )
            registry = WaitingQueueRegistry(applicationContext)

            // act & assert
            assertThrows<IllegalArgumentException> {
                registry.afterSingletonsInstantiated()
            }
        }

        @Test
        fun `모든 값이 유효하면 정상 등록된다`() {
            // arrange
            val applicationContext = mockk<ApplicationContext>()
            every { applicationContext.getBeansWithAnnotation(RestController::class.java) } returns mapOf(
                "testController" to ValidController(),
            )
            registry = WaitingQueueRegistry(applicationContext)

            // act
            registry.afterSingletonsInstantiated()

            // assert
            val config = registry.getQueueConfig("valid-queue")
            assert(config != null)
            assert(config!!.name == "valid-queue")
            assert(config.throughputPerServerPerSecond == 100)
            assert(config.activeTokenTTLSeconds == 300)
        }
    }
}

class InvalidThroughputController {
    @WaitingQueue(name = "invalid-queue", throughputPerServerPerSecond = 0, activeTokenTTLSeconds = 300)
    fun issue() {
        // dummy
    }
}

class InvalidTTLController {
    @WaitingQueue(name = "invalid-queue", throughputPerServerPerSecond = 100, activeTokenTTLSeconds = -1)
    fun issue() {
        // dummy
    }
}

class InvalidNameController {
    @WaitingQueue(name = "", throughputPerServerPerSecond = 100, activeTokenTTLSeconds = 300)
    fun issue() {
        // dummy
    }
}

class ValidController {
    @WaitingQueue(name = "valid-queue", throughputPerServerPerSecond = 100, activeTokenTTLSeconds = 300)
    fun issue() {
        // dummy
    }
}
