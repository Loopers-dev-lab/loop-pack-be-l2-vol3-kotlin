package com.loopers.infrastructure.ranking

import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class RedisRankingRepositoryTest @Autowired constructor(
    private val redisRankingRepository: RedisRankingRepository,
    private val redisTemplate: RedisTemplate<String, String>,
    private val redisCleanUp: RedisCleanUp,
) {

    private val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
    private val key = "${RedisRankingConstants.RANKING_KEY_PREFIX}${today.format(DateTimeFormatter.BASIC_ISO_DATE)}"

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    @Nested
    @DisplayName("getTopN")
    inner class GetTopN {

        @Test
        @DisplayName("ZREVRANGE로 지정 범위의 상품 ID + score가 내림차순으로 조회된다")
        fun `점수 내림차순으로 조회된다`() {
            // Arrange
            redisTemplate.opsForZSet().add(key, "1", 1.0)
            redisTemplate.opsForZSet().add(key, "2", 3.0)
            redisTemplate.opsForZSet().add(key, "3", 2.0)

            // Act
            val result = redisRankingRepository.getTopN(today, 0, 3)

            // Assert
            assertThat(result).hasSize(3)
            assertThat(result[0].productId).isEqualTo(2L)
            assertThat(result[0].score).isCloseTo(3.0, Offset.offset(0.001))
            assertThat(result[1].productId).isEqualTo(3L)
            assertThat(result[2].productId).isEqualTo(1L)
        }

        @Test
        @DisplayName("offset과 limit으로 페이지네이션이 동작한다")
        fun `페이지네이션이 동작한다`() {
            // Arrange
            for (i in 1..5) {
                redisTemplate.opsForZSet().add(key, i.toString(), i.toDouble())
            }

            // Act — offset=2이면 3번째부터 (5,4 건너뛰고 3,2)
            val result = redisRankingRepository.getTopN(today, 2, 2)

            // Assert
            assertThat(result).hasSize(2)
            assertThat(result[0].productId).isEqualTo(3L)
            assertThat(result[1].productId).isEqualTo(2L)
        }

        @Test
        @DisplayName("동점 시 member 문자열 lexicographic 내림차순으로 정렬된다")
        fun `동점 시 lex 내림차순으로 정렬된다`() {
            // Arrange — "10"과 "2"는 lex 순서로 "10" < "2", ZREVRANGE는 전체 역순이므로 "2" 먼저
            redisTemplate.opsForZSet().add(key, "10", 5.0)
            redisTemplate.opsForZSet().add(key, "2", 5.0)
            redisTemplate.opsForZSet().add(key, "3", 5.0)

            // Act
            val result = redisRankingRepository.getTopN(today, 0, 3)

            // Assert — lex DESC: "3" > "2" > "10"
            assertThat(result).hasSize(3)
            assertThat(result[0].productId).isEqualTo(3L)
            assertThat(result[1].productId).isEqualTo(2L)
            assertThat(result[2].productId).isEqualTo(10L)
        }

        @Test
        @DisplayName("키가 없으면 빈 리스트를 반환한다")
        fun `키가 없으면 빈 리스트를 반환한다`() {
            // Act
            val result = redisRankingRepository.getTopN(today, 0, 10)

            // Assert
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("getRank")
    inner class GetRank {

        @Test
        @DisplayName("ZREVRANK로 특정 상품의 1-based 순위가 반환된다")
        fun `1-based 순위가 반환된다`() {
            // Arrange
            redisTemplate.opsForZSet().add(key, "1", 1.0)
            redisTemplate.opsForZSet().add(key, "2", 3.0)
            redisTemplate.opsForZSet().add(key, "3", 2.0)

            // Act & Assert
            assertThat(redisRankingRepository.getRank(today, 2L)).isEqualTo(1)
            assertThat(redisRankingRepository.getRank(today, 3L)).isEqualTo(2)
            assertThat(redisRankingRepository.getRank(today, 1L)).isEqualTo(3)
        }

        @Test
        @DisplayName("랭킹에 없는 상품은 null을 반환한다")
        fun `없는 상품은 null을 반환한다`() {
            // Act
            val result = redisRankingRepository.getRank(today, 999L)

            // Assert
            assertThat(result).isNull()
        }
    }
}
