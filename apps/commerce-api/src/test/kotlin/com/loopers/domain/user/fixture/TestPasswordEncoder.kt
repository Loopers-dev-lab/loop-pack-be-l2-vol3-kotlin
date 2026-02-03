package com.loopers.domain.user.fixture

import com.loopers.domain.user.PasswordEncoder

class TestPasswordEncoder : PasswordEncoder {
    override fun encode(rawPassword: String): String = "encoded_$rawPassword"
    override fun matches(rawPassword: String, encodedPassword: String): Boolean =
        encodedPassword == "encoded_$rawPassword"
}
