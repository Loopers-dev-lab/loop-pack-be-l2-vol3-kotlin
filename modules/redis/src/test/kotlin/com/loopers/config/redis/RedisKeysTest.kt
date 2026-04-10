package com.loopers.config.redis

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("RedisKeys")
class RedisKeysTest {

    @DisplayName("rankingKey 생성 시,")
    @Nested
    inner class RankingKey {

        @DisplayName("ranking:all:{date} 형식의 키를 반환한다.")
        @Test
        fun returnsRankingKeyWithDate() {
            // arrange
            val date = "20260408"

            // act
            val key = RedisKeys.rankingKey(date)

            // assert
            assertThat(key).isEqualTo("ranking:all:20260408")
        }
    }
}
