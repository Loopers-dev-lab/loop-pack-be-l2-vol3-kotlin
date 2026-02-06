package com.loopers.domain.member

/**
 * 비밀번호 암호화 인터페이스
 * - 도메인 레이어에서 정의
 * - 구현체는 infrastructure 레이어에서 제공 (예: BCrypt)
 */
interface PasswordEncoder {

    /**
     * 평문 비밀번호를 암호화한다.
     */
    fun encode(rawPassword: String): String

    /**
     * 평문 비밀번호와 암호화된 비밀번호가 일치하는지 확인한다.
     */
    fun matches(rawPassword: String, encodedPassword: String): Boolean
}
