package com.loopers.interfaces.consumer.fixture

import com.loopers.application.event.IdempotencyService
import com.loopers.config.kafka.AvroSchemaProvider
import io.mockk.every
import io.mockk.mockk

object ConsumerTestFixtures {

    /**
     * 정본 스키마(modules/kafka/src/main/resources/avro 하위 .avsc) 기반 레코드 빌더.
     * 테스트에서 스키마를 수제 재선언하면 정본과 드리프트한다.
     */
    val avroSchemaProvider = AvroSchemaProvider()

    /** 멱등 체크를 통과시키고 비즈니스 로직 람다를 그대로 실행하는 스텁. */
    fun passThroughIdempotencyService(): IdempotencyService {
        val service = mockk<IdempotencyService>()
        every { service.executeWithIdempotency(any(), any(), any()) } answers {
            thirdArg<() -> Unit>().invoke()
            true
        }
        return service
    }
}
