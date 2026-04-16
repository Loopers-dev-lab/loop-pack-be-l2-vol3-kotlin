package com.loopers

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@TestPropertySource(properties = ["spring.batch.job.enabled=false"])
class CommerceBatchApplicationTest {
    @Test
    fun contextLoads() {}
}
