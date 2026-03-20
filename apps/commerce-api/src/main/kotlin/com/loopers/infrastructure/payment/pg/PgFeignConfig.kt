package com.loopers.infrastructure.payment.pg

import feign.Request
import org.springframework.context.annotation.Bean
import java.util.concurrent.TimeUnit

class PgFeignConfig {

    @Bean
    fun requestOptions(): Request.Options {
        return Request.Options(
            1L, TimeUnit.SECONDS,   // connectTimeout: TCP 핸드셰이크 기준, 1초 안에 연결 안 되면 PG 네트워크 문제
            5L, TimeUnit.SECONDS,   // readTimeout: PG 카드사 통신(2~3s) + 네트워크 지연 여유. Retry 3회 감안해 최대 16s 이내 응답
            true,                   // followRedirects
        )
    }
}
