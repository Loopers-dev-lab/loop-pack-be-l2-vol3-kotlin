package com.loopers.config

import com.loopers.support.EmbeddedKafkaTestSupport
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.kafka.test.utils.KafkaTestUtils
import java.time.Duration

class DlqIntegrationTest : EmbeddedKafkaTestSupport() {

    @DisplayName("DLQ 통합 테스트:")
    @Nested
    inner class DlqBehavior {

        @DisplayName("반복 실패하는 메시지가 catalog-events.dlq로 이동한다.")
        @Test
        fun movesFailedMessageToCatalogDlq() {
            // arrange: Consumer 파티션 할당 대기 후 DLQ consumer 생성
            waitForConsumerAssignment()
            val dlqConsumer = createStringConsumer("dlq-catalog-verify")
            embeddedKafka.consumeFromAnEmbeddedTopic(dlqConsumer, "catalog-events.dlq")

            // act: catalog-events 토픽에 파싱 불가능한 메시지 발행
            sendStringMessage("catalog-events", "test-key", "invalid-json")

            // assert: 재시도(3회) 소진 후 catalog-events.dlq에서 메시지 확인
            val records = KafkaTestUtils.getRecords(dlqConsumer, Duration.ofSeconds(30))
            val dlqRecords = records.records("catalog-events.dlq").toList()
            assertThat(dlqRecords).isNotEmpty
            assertThat(dlqRecords.first().key()).isEqualTo("test-key")

            dlqConsumer.close()
        }
    }
}
