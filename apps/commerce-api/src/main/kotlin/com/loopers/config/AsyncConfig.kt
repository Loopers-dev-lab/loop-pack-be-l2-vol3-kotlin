package com.loopers.config

import org.slf4j.LoggerFactory
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.AsyncConfigurer
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.lang.reflect.Method
import java.util.concurrent.Executor

@Configuration
@EnableAsync
class AsyncConfig : AsyncConfigurer {

    @Bean("eventTaskExecutor")
    fun eventTaskExecutor(): ThreadPoolTaskExecutor {
        return ThreadPoolTaskExecutor().apply {
            corePoolSize = 2
            maxPoolSize = 5
            queueCapacity = 100
            setThreadNamePrefix("event-")
            initialize()
        }
    }

    override fun getAsyncExecutor(): Executor {
        return eventTaskExecutor()
    }

    override fun getAsyncUncaughtExceptionHandler(): AsyncUncaughtExceptionHandler {
        return AsyncEventExceptionHandler()
    }

    private class AsyncEventExceptionHandler : AsyncUncaughtExceptionHandler {
        private val log = LoggerFactory.getLogger(AsyncEventExceptionHandler::class.java)

        override fun handleUncaughtException(ex: Throwable, method: Method, vararg params: Any?) {
            log.error("비동기 이벤트 처리 실패 - method: {}, params: {}", method.name, params.toList(), ex)
        }
    }
}
