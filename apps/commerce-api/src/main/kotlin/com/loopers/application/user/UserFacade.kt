package com.loopers.application.user

import com.loopers.application.coupon.UserCouponInfo
import com.loopers.domain.coupon.UserCouponService
import com.loopers.domain.user.BirthDate
import com.loopers.domain.user.Password
import com.loopers.domain.user.PasswordEncryptor
import com.loopers.domain.user.UserService
import com.loopers.domain.user.UserValidator
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class UserFacade(
    private val userService: UserService,
    private val userValidator: UserValidator,
    private val passwordEncryptor: PasswordEncryptor,
    private val userCouponService: UserCouponService,
) {
    @Transactional
    fun register(
        loginId: String,
        password: String,
        name: String,
        birthDate: String,
        email: String,
    ): UserInfo {
        userValidator.validateNoBirthDate(Password(password), BirthDate(birthDate))
        val encrypted = passwordEncryptor.encrypt(password)
        return userService.createUser(
            loginId = loginId,
            encryptedPassword = encrypted,
            name = name,
            birthDate = birthDate,
            email = email,
        ).let { UserInfo.from(it) }
    }

    fun getUserInfo(loginId: String): UserInfo {
        return userService.getUserByLoginId(loginId)
            .let { UserInfo.from(it) }
    }

    @Transactional(readOnly = true)
    fun getMyCoupons(loginId: String): List<UserCouponInfo> {
        val user = userService.getUserByLoginId(loginId)
        return userCouponService.getUserCouponsByUserId(user.id)
            .map { UserCouponInfo.from(it) }
    }

    @Transactional
    fun changePassword(
        loginId: String,
        newPassword: String,
        birthDate: String,
    ) {
        userValidator.validateNoBirthDate(Password(newPassword), BirthDate(birthDate))
        val encrypted = passwordEncryptor.encrypt(newPassword)
        userService.updatePassword(loginId, encrypted)
    }
}
