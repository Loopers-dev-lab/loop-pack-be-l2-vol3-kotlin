package com.loopers.domain.user

class User private constructor(
    val persistenceId: Long?,
    val loginId: LoginId,
    val password: Password,
    val name: Name,
    val birthDate: BirthDate,
    val email: Email,
    val gender: GenderType,
) {
    /**
     * Verifies that the given raw password matches the user's stored password.
     *
     * @param rawPassword The plaintext password to verify.
     * @param encoder The PasswordEncoder used to validate the password.
     * @return `true` if the provided password matches the stored password, `false` otherwise.
     */
    fun authenticate(rawPassword: String, encoder: PasswordEncoder): Boolean {
        return password.matches(rawPassword, encoder)
    }

    /**
     * Change the user's password and return a new User instance with the updated password.
     *
     * @param oldPassword The current plain-text password used for verification.
     * @param newPassword The new plain-text password to set; must differ from the current password and must not contain the user's birth date.
     * @return A new User identical to the original except its `password` has been replaced with one created from `newPassword`.
     * @throws IllegalArgumentException if the provided `oldPassword` does not match the current password, if `newPassword` matches the current password, or if `newPassword` contains the user's birth date.
     */
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
        require(!newPassword.contains(birthDate.toCompactString())) {
            "비밀번호에 생년월일을 포함할 수 없습니다."
        }

        return User(
            persistenceId = persistenceId,
            loginId = loginId,
            password = Password.create(newPassword, encoder),
            name = name,
            birthDate = birthDate,
            email = email,
            gender = gender,
        )
    }

    companion object {
        /**
         * Creates a new User for signup with no persistence id.
         *
         * The raw password is converted into a domain Password using the provided encoder.
         *
         * @param rawPassword The plaintext password supplied during registration.
         * @param encoder The password encoder used to create the stored Password.
         * @return A User instance with `persistenceId = null`.
         * @throws IllegalArgumentException if `rawPassword` contains the user's birth date.
         */
        fun register(
            loginId: LoginId,
            rawPassword: String,
            name: Name,
            birthDate: BirthDate,
            email: Email,
            gender: GenderType,
            encoder: PasswordEncoder,
        ): User {
            require(!rawPassword.contains(birthDate.toCompactString())) {
                "비밀번호에 생년월일을 포함할 수 없습니다."
            }
            return User(
                persistenceId = null,
                loginId = loginId,
                password = Password.create(rawPassword, encoder),
                name = name,
                birthDate = birthDate,
                email = email,
                gender = gender,
            )
        }

        /**
         * Reconstructs a User instance from persisted data without performing domain validations.
         *
         * @param persistenceId The persistent identifier from the data store.
         * @param loginId The user's login identifier.
         * @param password The user's persisted Password value object.
         * @param name The user's Name value object.
         * @param birthDate The user's BirthDate value object.
         * @param email The user's Email value object.
         * @param gender The user's GenderType value object.
         * @return A User populated with the provided persisted values.
         */
        fun reconstitute(
            persistenceId: Long,
            loginId: LoginId,
            password: Password,
            name: Name,
            birthDate: BirthDate,
            email: Email,
            gender: GenderType,
        ): User {
            return User(
                persistenceId = persistenceId,
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