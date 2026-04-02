package com.loopers.infrastructure.queue

import com.loopers.testcontainers.RedisTestContainersConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(RedisTestContainersConfig::class)
class WaitingQueueRegistryTest @Autowired constructor(
    private val waitingQueueRegistry: WaitingQueueRegistry,
) {

    @DisplayName("WaitingQueueRegistry - 큐 이름 목록")
    @Test
    fun `OrderV1Controller에 선언된 order-queue가 스캔된다`() {
        val queueNames = waitingQueueRegistry.getQueueConfigs().map { it.name }
        assertThat(queueNames).contains("order-queue")
    }

    @DisplayName("WaitingQueueRegistry - 큐 설정 조회")
    @Test
    fun `order-queue의 설정이 어노테이션 값과 일치한다`() {
        val config = waitingQueueRegistry.getQueueConfig("order-queue")
        assertThat(config).isNotNull()
        assertThat(config!!.name).isEqualTo("order-queue")
        assertThat(config.throughputPerServerPerSecond).isEqualTo(175)
        assertThat(config.activeTokenTTLSeconds).isEqualTo(300)
    }

    @DisplayName("WaitingQueueRegistry - 존재하지 않는 큐")
    @Test
    fun `등록되지 않은 큐 이름은 null을 반환한다`() {
        val config = waitingQueueRegistry.getQueueConfig("non-existent-queue")
        assertThat(config).isNull()
    }
}
