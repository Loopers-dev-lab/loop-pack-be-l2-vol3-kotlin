package com.loopers.domain.user

interface PasswordEncoder {

    /**
 * Produces an encoded representation of the given plain-text password.
 *
 * @param rawPassword The plain-text password to encode.
 * @return The encoded password string.
 */
fun encode(rawPassword: String): String

    /**
 * Determines whether the provided raw password corresponds to the stored encoded password.
 *
 * @param rawPassword the plaintext password to verify
 * @param encodedPassword the encoded (stored) password to compare against
 * @return `true` if `rawPassword`, when encoded, matches `encodedPassword`, `false` otherwise
 */
fun matches(rawPassword: String, encodedPassword: String): Boolean
}