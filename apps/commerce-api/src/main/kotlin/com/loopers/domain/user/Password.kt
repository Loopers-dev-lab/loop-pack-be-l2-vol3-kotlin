package com.loopers.domain.user

class Password private constructor(private val value: String) {

    /**
     * Checks whether a raw password corresponds to the stored encoded password.
     *
     * @param rawPassword The plain-text password to verify.
     * @param encoder The PasswordEncoder used to compare the plain-text password with the stored encoded value.
     * @return `true` if `rawPassword` matches the stored encoded password, `false` otherwise.
     */
    fun matches(rawPassword: String, encoder: PasswordEncoder): Boolean {
        return encoder.matches(rawPassword, value)
    }

    /**
 * Exposes the stored encoded password for persistence mapping.
 *
 * @return The encoded password string.
 */
    fun toEncodedString(): String = value

    /**
 * Provides a masked representation of the password value.
 *
 * @return The constant string "Password(****)" to avoid exposing the actual password.
 */
override fun toString(): String = "Password(****)"

    companion object {
        private const val MIN_LENGTH = 8
        private const val MAX_LENGTH = 16
        private val ALLOWED_CHARS_REGEX = Regex("^[a-zA-Z0-9!@#\$%^&*()_+\\-=]+$")

        /**
         * Create a Password by validating the raw password and encoding it.
         *
         * @param rawPassword The plain-text password to validate and encode. Must be 8–16 characters long and contain only letters (a–z, A–Z), digits (0–9), or the symbols ! @ # $ % ^ & * ( ) _ + - =.
         * @param encoder The PasswordEncoder used to encode the validated raw password.
         * @return A Password instance containing the encoded password.
         * @throws IllegalArgumentException If the raw password fails length or allowed-character validation.
         */
        fun create(rawPassword: String, encoder: PasswordEncoder): Password {
            validate(rawPassword)
            return Password(encoder.encode(rawPassword))
        }

        /**
         * Constructs a Password from an already encoded password string for restoring from persistence.
         *
         * This factory does not validate or re-encode the input; it is intended to be used only by
         * Infrastructure-layer mappers when reconstructing a Password from stored data.
         *
         * @param encoded The encoded password string retrieved from the database.
         * @return A Password wrapping the provided encoded value.
         */
        fun fromEncoded(encoded: String): Password {
            return Password(encoded)
        }

        /**
         * Validates a raw password's length and allowed characters.
         *
         * @param rawPassword The plaintext password to validate.
         * @throws IllegalArgumentException if the password is shorter than MIN_LENGTH, longer than MAX_LENGTH, or contains characters outside the allowed set.
         */
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