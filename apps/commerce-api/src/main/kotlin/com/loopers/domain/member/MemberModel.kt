package com.loopers.domain.member

import com.loopers.domain.member.vo.MemberName
import java.time.LocalDate
import java.time.ZonedDateTime

data class MemberModel(
    val id: Long = 0,
    val loginId: String,
    val password: String,
    val name: String,
    val birthday: LocalDate,
    val email: String,
    val createdAt: ZonedDateTime? = null,
    val updatedAt: ZonedDateTime? = null,
    val deletedAt: ZonedDateTime? = null,
) {
    fun changePassword(encodedPassword: String): MemberModel =
        copy(password = encodedPassword)

    fun getMaskedName(): String = MemberName(name).masked()
}
