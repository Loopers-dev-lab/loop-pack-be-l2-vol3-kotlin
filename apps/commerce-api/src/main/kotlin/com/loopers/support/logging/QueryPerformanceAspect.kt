package com.loopers.support.logging

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Aspect
@Component
@Profile("local")
class QueryPerformanceAspect {
    private val log = LoggerFactory.getLogger(javaClass)

    @Around(
        "execution(* com.loopers.infrastructure.product.ProductRepositoryImpl.findAllByCondition(..)) || " +
            "execution(* com.loopers.infrastructure.product.ProductRepositoryImpl.findByIdAndDeletedAtIsNull(..))",
    )
    fun measureQueryTime(joinPoint: ProceedingJoinPoint): Any? {
        val methodName = joinPoint.signature.name
        val args = joinPoint.args.joinToString(", ")

        val start = System.nanoTime()
        val result = joinPoint.proceed()
        val elapsedMs = (System.nanoTime() - start) / 1_000_000

        log.info("[PERF] {}.{}({}) took {}ms", joinPoint.target.javaClass.simpleName, methodName, args, elapsedMs)
        return result
    }
}
