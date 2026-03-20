package com.loopers.infrastructure.pg

import feign.Request
import feign.Retryer
import org.springframework.context.annotation.Bean
import java.util.concurrent.TimeUnit

class PgClientConfig {

    @Bean
    fun feignOptions(): Request.Options {
        return Request.Options(
            // connectTimeout: 1초 — 정상 TCP 연결은 수십ms, 1초 초과 시 PG 자체 장애로 판단
            1000,
            TimeUnit.MILLISECONDS,
            // readTimeout: 3초 — PG 정상 응답(100~562ms)의 약 6배 여유, 너무 길면 스레드 점유 증가
            3000,
            TimeUnit.MILLISECONDS,
            true,
        )
    }

    @Bean
    fun feignRetryer(): Retryer {
        return Retryer.NEVER_RETRY // Feign 자체 Retry 비활성화 — 재시도는 Resilience4j Retry가 담당
    }
}
