package com.loopers.domain.member

import java.time.LocalDate

/**
 * 회원가입 요청 커맨드
 *
 * 📌 Kotlin 설명: data class
 * - Java의 POJO + Lombok(@Data)과 유사
 * - equals(), hashCode(), toString(), copy() 자동 생성
 * - 불변 데이터 전달 객체(DTO)에 적합
 */
data class SignUpCommand(
    val loginId: String,
    val password: String,
    val name: String,
    val birthDate: LocalDate,
    val email: String,
)
