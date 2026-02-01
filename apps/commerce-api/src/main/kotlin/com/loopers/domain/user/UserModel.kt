package com.loopers.domain.user

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDate


@Entity
@Table(name = "user")
class UserModel(
    userId: String,
    password: String,
    name: String,
    birthDate: LocalDate,
    email: String,
) : BaseEntity() {

    var userId: String = userId
        protected set

    var password: String = password
        protected set

    var name: String = name
        protected set

    var birthDate: LocalDate = birthDate
        protected set

    var email: String = email
        protected set

    init {
        if (userId.isBlank()) throw CoreException(ErrorType.BAD_REQUEST, "아이디는 비어있을 수 없습니다.")
        if (password.isBlank()) throw CoreException(ErrorType.BAD_REQUEST, "암호는 비어있을 수 없습니다.")
        if (name.isBlank()) throw CoreException(ErrorType.BAD_REQUEST, "이름는 비어있을 수 없습니다.")
        if (birthDate.isAfter(LocalDate.now())) throw CoreException(ErrorType.BAD_REQUEST, "생년월일은 현재보다 미래일 수 없습니다.")
        if (email.isBlank()) throw CoreException(ErrorType.BAD_REQUEST, "이메일은 비어있을 수 없습니다.")
    }
}
