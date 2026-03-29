package com.loopers.config.async

import org.slf4j.LoggerFactory
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@EnableAsync
@EnableScheduling
@Configuration
class AsyncConfig : AsyncConfigurer {

    companion object {
        private val log = LoggerFactory.getLogger(AsyncConfig::class.java)
        const val EVENT_ASYNC_EXECUTOR = "eventAsyncExecutor"
    }

    /**
     * 이벤트 처리 전용 스레드풀.
     *
     * - core=2, max=10, queue=500: 회사 코드(campaign-event-async)와 동일 수준
     * - 이벤트 리스너(@Async)가 이 풀을 사용
     * - 메인 요청 스레드와 격리되어 이벤트 처리 지연이 API 응답에 영향 없음
     */
    @Bean(EVENT_ASYNC_EXECUTOR)
    fun eventAsyncExecutor(): Executor {
        return ThreadPoolTaskExecutor().apply {
            corePoolSize = 2
            maxPoolSize = 10
            queueCapacity = 500
            setThreadNamePrefix("event-async-")
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(30)
            initialize()
        }
    }

    override fun getAsyncUncaughtExceptionHandler(): AsyncUncaughtExceptionHandler {
        return AsyncUncaughtExceptionHandler { ex, method, params ->
            log.error(
                "비동기 이벤트 처리 실패 [method={}, params={}]",
                method.name,
                params.contentToString(),
                ex,
            )
        }
    }
}
