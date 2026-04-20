package com.loopers

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource

/**
 * commerce-batch는 `spring.batch.job.name=${job.name:NONE}` 기본값 때문에 컨텍스트 로드 시
 * JobLauncherApplicationRunner가 'NONE' 이름의 Job을 찾다 실패한다. 컨텍스트 로드 검증 용도이므로
 * 해당 autoconfig를 비활성화한 상태로 띄운다.
 */
@SpringBootTest
@TestPropertySource(properties = ["spring.batch.job.enabled=false"])
class CommerceBatchApplicationTest {
    @Test
    fun contextLoads() {}
}
