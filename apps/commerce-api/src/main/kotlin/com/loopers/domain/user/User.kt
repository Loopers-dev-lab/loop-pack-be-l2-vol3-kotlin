package com.loopers.domain.user

class User private constructor(
    val id: Long?,
    val loginId: LoginId,
    val password: Password,
    val name: Name,
    val birthDate: BirthDate,
    val email: Email,
    val gender: GenderType,
) {
    fun authenticate(rawPassword: String, encoder: PasswordEncoder): Boolean {
        return password.matches(rawPassword, encoder)
    }

    fun changePassword(
        oldPassword: String,
        newPassword: String,
        encoder: PasswordEncoder,
    ): User {
        require(password.matches(oldPassword, encoder)) {
            "기존 비밀번호가 일치하지 않습니다."
        }
        require(!password.matches(newPassword, encoder)) {
            "새 비밀번호는 기존과 달라야 합니다."
        }

        val newPasswordVo = Password.create(newPassword, birthDate, encoder)
        return User(
            id = id,
            loginId = loginId,
            password = newPasswordVo,
            name = name,
            birthDate = birthDate,
            email = email,
            gender = gender,
        )
    }

    companion object {
        /**
         * 회원가입 시 사용.
         * Application layer에서 호출.
         */
        fun register(
            loginId: LoginId,
            password: Password,
            name: Name,
            birthDate: BirthDate,
            email: Email,
            gender: GenderType,
        ): User {
            return User(
                id = null,
                loginId = loginId,
                password = password,
                name = name,
                birthDate = birthDate,
                email = email,
                gender = gender,
            )
        }

        /**
         * DB에서 복원할 때만 사용한다.
         * Infrastructure layer의 Mapper에서만 호출할 것.
         */
        fun reconstitute(
            id: Long,
            loginId: LoginId,
            password: Password,
            name: Name,
            birthDate: BirthDate,
            email: Email,
            gender: GenderType,
        ): User {
            return User(
                id = id,
                loginId = loginId,
                password = password,
                name = name,
                birthDate = birthDate,
                email = email,
                gender = gender,
            )
        }
    }
}
