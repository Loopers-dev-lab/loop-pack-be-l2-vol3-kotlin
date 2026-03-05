package com.loopers.application.error

import com.loopers.domain.error.CoreException
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component

@Aspect
@Component
class DomainExceptionTranslator {
    @Around("within(com.loopers.application..*Facade)")
    fun translate(joinPoint: ProceedingJoinPoint): Any? {
        try {
            return joinPoint.proceed()
        } catch (e: CoreException) {
            throw ApplicationException.from(e)
        }
    }
}
