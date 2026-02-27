package com.loopers.domain.user

import com.loopers.domain.user.dto.UserInfo
import com.loopers.domain.user.dto.SignUpCommand
import com.loopers.domain.user.vo.BirthDate
import com.loopers.domain.user.vo.Email
import com.loopers.domain.user.vo.LoginId
import com.loopers.domain.user.vo.Name
import com.loopers.domain.user.vo.Password
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    @Transactional
    fun signUp(command: SignUpCommand) {
        val loginId = LoginId.of(command.loginId)
        if (userRepository.existsByLoginId(loginId)) {
            throw CoreException(ErrorType.BAD_REQUEST, "사용할 수 없는 로그인ID 입니다.")
        }

        Password.validate(command.password, command.birthDate)

        val user = User.create(
            loginId = loginId,
            password = Password.ofEncrypted(passwordEncoder.encode(command.password)),
            name = Name.of(command.name),
            birthDate = BirthDate.of(command.birthDate),
            email = Email.of(command.email),
        )

        userRepository.save(user)
    }

    fun findUserInfo(id: Long): UserInfo {
        val findUser = findUser(id)
        return UserInfo.from(findUser)
    }

    @Transactional
    fun changePassword(id: Long, currentPassword: String, newPassword: String) {
        val findUser = findUser(id)

        if (!passwordEncoder.matches(currentPassword, findUser.password.value)) {
            throw CoreException(ErrorType.BAD_REQUEST, "기존 비밀번호가 일치하지 않습니다")
        }

        if (passwordEncoder.matches(newPassword, findUser.password.value)) {
            throw CoreException(ErrorType.BAD_REQUEST, "현재 비밀번호와 동일한 비밀번호는 사용할 수 없습니다")
        }

        Password.validate(newPassword, findUser.birthDate.value)
        findUser.changePassword(Password.ofEncrypted(passwordEncoder.encode(newPassword)))
    }

    fun getUser(id: Long): User =
        findUser(id)

    private fun findUser(id: Long): User {
        val findUser = userRepository.findUserById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "사용자 정보가 없습니다")
        return findUser
    }
}
