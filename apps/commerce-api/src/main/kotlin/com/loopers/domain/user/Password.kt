package com.loopers.domain.user

class Password private constructor(private val value: String) {

    fun matches(rawPassword: String, encoder: PasswordEncoder): Boolean {
        return encoder.matches(rawPassword, value)
    }

    /**
     * Persistence 변환용.
     * Infrastructure layer의 Mapper에서만 사용할 것.
     */
    fun toEncodedString(): String = value

    override fun toString(): String = "Password(****)"

    companion object {
        private const val MIN_LENGTH = 8
        private const val MAX_LENGTH = 16
        private val ALLOWED_CHARS_REGEX = Regex("^[a-zA-Z0-9!@#\$%^&*()_+\\-=]+$")

        fun create(rawPassword: String, encoder: PasswordEncoder): Password {
            validate(rawPassword)
            return Password(encoder.encode(rawPassword))
        }

        /**
         * DB에서 복원할 때만 사용.
         * Infrastructure layer의 Mapper에서만 호출할 것.
         */
        fun fromEncoded(encoded: String): Password {
            return Password(encoded)
        }

        private fun validate(rawPassword: String) {
            require(rawPassword.length >= MIN_LENGTH) {
                "비밀번호는 ${MIN_LENGTH}자 이상이어야 합니다."
            }
            require(rawPassword.length <= MAX_LENGTH) {
                "비밀번호는 ${MAX_LENGTH}자 이하여야 합니다."
            }
            require(ALLOWED_CHARS_REGEX.matches(rawPassword)) {
                "비밀번호는 영문 대소문자, 숫자, 특수문자(!@#\$%^&*()_+-=)만 허용됩니다."
            }
        }
    }
}
