package com.loopers.infrastructure.queue

import com.loopers.support.annotation.WaitingQueue
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
            assertNotNull(config)
            assertEquals("valid-queue", config?.name)
            assertEquals(100, config?.throughputPerServerPerSecond)
            assertEquals(300, config?.activeTokenTTLSeconds)
        }

        @Test
        fun `경계값(throughputPerServerPerSecond=1, activeTokenTTLSeconds=1)도 정상 등록된다`() {
            // arrange
            val applicationContext = mockk<ApplicationContext>()
            every { applicationContext.getBeansWithAnnotation(RestController::class.java) } returns mapOf(
                "testController" to BoundaryQueueController(),
            )
            registry = WaitingQueueRegistry(applicationContext)

            // act
            registry.afterSingletonsInstantiated()

            // assert
            val config = registry.getQueueConfig("boundary-queue")
            assertNotNull(config)
            assertEquals("boundary-queue", config?.name)
            assertEquals(1, config?.throughputPerServerPerSecond)
            assertEquals(1, config?.activeTokenTTLSeconds)
        }
    }

    @DisplayName("동일한 큐 이름 검증")
    @Nested
    inner class ConflictingQueueConfigTest {

        @Test
        fun `같은 큐 이름이지만 throughput이 다르면 예외를 발생시킨다`() {
            // arrange
            val applicationContext = mockk<ApplicationContext>()
            every { applicationContext.getBeansWithAnnotation(RestController::class.java) } returns mapOf(
                "controller1" to ConflictingThroughputController1(),
                "controller2" to ConflictingThroughputController2(),
            )
            registry = WaitingQueueRegistry(applicationContext)

            // act & assert
            assertThrows<IllegalStateException> {
                registry.afterSingletonsInstantiated()
            }
        }

        @Test
        fun `같은 큐 이름이지만 TTL이 다르면 예외를 발생시킨다`() {
            // arrange
            val applicationContext = mockk<ApplicationContext>()
            every { applicationContext.getBeansWithAnnotation(RestController::class.java) } returns mapOf(
                "controller1" to ConflictingTTLController1(),
                "controller2" to ConflictingTTLController2(),
            )
            registry = WaitingQueueRegistry(applicationContext)

            // act & assert
            assertThrows<IllegalStateException> {
                registry.afterSingletonsInstantiated()
            }
        }

        @Test
        fun `같은 큐 이름이고 모든 설정이 일치하면 정상 등록된다`() {
            // arrange
            val applicationContext = mockk<ApplicationContext>()
            every { applicationContext.getBeansWithAnnotation(RestController::class.java) } returns mapOf(
                "controller1" to IdenticalConfigController1(),
                "controller2" to IdenticalConfigController2(),
            )
            registry = WaitingQueueRegistry(applicationContext)

            // act
            registry.afterSingletonsInstantiated()

            // assert
            val config = registry.getQueueConfig("same-queue")
            assertNotNull(config)
            assertEquals(100, config?.throughputPerServerPerSecond)
            assertEquals(300, config?.activeTokenTTLSeconds)
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

class ConflictingThroughputController1 {
    @WaitingQueue(name = "conflict-queue", throughputPerServerPerSecond = 100, activeTokenTTLSeconds = 300)
    fun issue() {
        // dummy
    }
}

class ConflictingThroughputController2 {
    @WaitingQueue(name = "conflict-queue", throughputPerServerPerSecond = 200, activeTokenTTLSeconds = 300)
    fun issue() {
        // dummy
    }
}

class ConflictingTTLController1 {
    @WaitingQueue(name = "conflict-queue", throughputPerServerPerSecond = 100, activeTokenTTLSeconds = 300)
    fun issue() {
        // dummy
    }
}

class ConflictingTTLController2 {
    @WaitingQueue(name = "conflict-queue", throughputPerServerPerSecond = 100, activeTokenTTLSeconds = 600)
    fun issue() {
        // dummy
    }
}

class IdenticalConfigController1 {
    @WaitingQueue(name = "same-queue", throughputPerServerPerSecond = 100, activeTokenTTLSeconds = 300)
    fun issue() {
        // dummy
    }
}

class IdenticalConfigController2 {
    @WaitingQueue(name = "same-queue", throughputPerServerPerSecond = 100, activeTokenTTLSeconds = 300)
    fun issue() {
        // dummy
    }
}

class BoundaryQueueController {
    @WaitingQueue(name = "boundary-queue", throughputPerServerPerSecond = 1, activeTokenTTLSeconds = 1)
    fun issue() {
        // dummy
    }
}
