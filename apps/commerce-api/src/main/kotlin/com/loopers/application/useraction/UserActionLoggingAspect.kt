package com.loopers.application.useraction

import com.loopers.domain.common.event.UserActionEvent
import com.loopers.interfaces.config.auth.AuthenticatedMember
import com.loopers.interfaces.config.auth.MemberAuthenticationInterceptor
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.AfterReturning
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Aspect
@Component
class UserActionLoggingAspect(
    private val eventPublisher: ApplicationEventPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @AfterReturning("@annotation(logAction)")
    fun logUserAction(joinPoint: JoinPoint, logAction: LogUserAction) {
        val memberId = extractMemberId(joinPoint) ?: return
        val targetId = extractTargetId(joinPoint) ?: return
        eventPublisher.publishEvent(
            UserActionEvent(
                memberId = memberId,
                actionType = logAction.action,
                targetType = logAction.targetType,
                targetId = targetId,
            ),
        )
    }

    private fun extractMemberId(joinPoint: JoinPoint): Long? {
        // 1. 메서드 인자에서 memberId 추출 (첫 번째 Long 파라미터)
        joinPoint.args.firstOrNull { it is Long }?.let { return it as Long }

        // 2. 요청 컨텍스트에서 인증된 회원 정보 추출
        return try {
            val requestAttributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
            val request = requestAttributes?.request
            val member = request?.getAttribute(
                MemberAuthenticationInterceptor.AUTHENTICATED_MEMBER_ATTRIBUTE,
            ) as? AuthenticatedMember
            member?.id
        } catch (e: Exception) {
            log.debug("유저 행동 로깅 스킵: memberId 추출 불가")
            null
        }
    }

    private fun extractTargetId(joinPoint: JoinPoint): Long? {
        // VIEW: getProduct(productId) → 첫 번째 Long
        // LIKE: like(memberId, productId) → 두 번째 Long
        val longArgs = joinPoint.args.filterIsInstance<Long>()
        return when {
            longArgs.size >= 2 -> longArgs[1] // memberId, targetId 순서
            longArgs.size == 1 -> longArgs[0] // targetId만
            else -> null
        }
    }
}
