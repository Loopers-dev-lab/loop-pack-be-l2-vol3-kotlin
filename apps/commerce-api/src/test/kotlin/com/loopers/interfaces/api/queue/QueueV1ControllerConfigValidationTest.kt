package com.loopers.interfaces.api.queue

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import com.loopers.application.queue.QueueFacade

@DisplayName("QueueV1Controller - 설정 검증 테스트")
class QueueV1ControllerConfigValidationTest {

    @DisplayName("throughputPerServerPerSecond = 0일 때 애플리케이션 시작 실패")
    @Test
    fun `throughputPerServerPerSecond가 0이면 validateConfiguration이 예외를 발생시킨다`() {
        val mockFacade = mockk<QueueFacade>()
        val controller = QueueV1Controller(mockFacade, 0)

        val exception = org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            controller.validateConfiguration()
        }

        assertThat(exception.message).contains("must be greater than 0")
        assertThat(exception.message).contains("got 0")
    }

    @DisplayName("throughputPerServerPerSecond < 0일 때 애플리케이션 시작 실패")
    @Test
    fun `throughputPerServerPerSecond가 음수이면 validateConfiguration이 예외를 발생시킨다`() {
        val mockFacade = mockk<QueueFacade>()
        val controller = QueueV1Controller(mockFacade, -10)

        val exception = org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            controller.validateConfiguration()
        }

        assertThat(exception.message).contains("must be greater than 0")
        assertThat(exception.message).contains("got -10")
    }

    @DisplayName("throughputPerServerPerSecond > 0일 때 검증 성공")
    @Test
    fun `throughputPerServerPerSecond가 양수이면 validateConfiguration이 성공한다`() {
        val mockFacade = mockk<QueueFacade>()
        val controller = QueueV1Controller(mockFacade, 175)

        // 예외 발생하지 않음
        controller.validateConfiguration()
    }
}
