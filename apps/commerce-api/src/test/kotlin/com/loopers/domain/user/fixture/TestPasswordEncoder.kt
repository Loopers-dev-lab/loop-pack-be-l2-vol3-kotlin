package com.loopers.domain.user.fixture

import com.loopers.domain.user.PasswordEncoder

class TestPasswordEncoder : PasswordEncoder {
    /**
 * Produces an encoded form of the provided raw password for tests.
 *
 * @param rawPassword The plaintext password to encode.
 * @return The encoded password (the test encoding starts with `encoded_` followed by the original password).
 */
override fun encode(rawPassword: String): String = "encoded_$rawPassword"
    /**
         * Checks whether an encoded password corresponds to the provided raw password.
         *
         * @param rawPassword The plaintext password to verify.
         * @param encodedPassword The encoded password to compare against.
         * @return `true` if `encodedPassword` equals the encoding of `rawPassword`, `false` otherwise.
         */
        override fun matches(rawPassword: String, encodedPassword: String): Boolean =
        encodedPassword == "encoded_$rawPassword"
}