package com.loopers.application.member

import com.loopers.infrastructure.config.CacheConfig
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.cache.CacheManager
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class MemberFacade(
    private val memberService: MemberService,
    private val cacheManager: CacheManager,
) {
    fun register(
        loginId: String,
        password: String,
        name: String,
        birthday: LocalDate,
        email: String,
    ): MemberInfo {
        try {
            val member = memberService.register(
                loginId = loginId,
                password = password,
                name = name,
                birthday = birthday,
                email = email,
            )
            return MemberInfo.from(member)
        } catch (e: DataIntegrityViolationException) {
            throw CoreException(ErrorType.CONFLICT, "이미 존재하는 로그인 ID입니다.")
        }
    }

    fun getMyInfo(loginId: String): MemberInfo {
        val member = memberService.getMemberByLoginId(loginId)
        return MemberInfo.from(member)
    }

    fun changePassword(loginId: String, newPassword: String): MemberInfo {
        memberService.changePassword(loginId, newPassword)
        cacheManager.getCache(CacheConfig.AUTH_CACHE)?.evict(loginId)
        val member = memberService.getMemberByLoginId(loginId)
        return MemberInfo.from(member)
    }
}
