package com.loopers.domain.user

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.Comment
import java.time.LocalDate

@Entity
@Table(name = "users")
@Comment("회원")
class User(
    loginId: String,
    password: String,
    name: String,
    birthday: LocalDate,
    email: String,
) : BaseEntity() {

    @Comment("로그인 ID (영문+숫자)")
    @Column(nullable = false, unique = true)
    var loginId: String = loginId
        protected set

    @Comment("비밀번호 (BCrypt)")
    @Column(nullable = false)
    var password: String = password
        protected set

    @Comment("회원명")
    @Column(nullable = false)
    var name: String = name
        protected set

    @Comment("생년월일")
    @Column(nullable = false)
    var birthday: LocalDate = birthday
        protected set

    @Comment("이메일")
    @Column(nullable = false)
    var email: String = email
        protected set

    init {
        if (!loginId.matches(Regex("^[a-zA-Z0-9]+$"))) {
            throw CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 영문과 숫자만 허용합니다.")
        }
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "이름은 필수입니다.")
        }
        if (!email.matches(Regex("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$"))) {
            throw CoreException(ErrorType.BAD_REQUEST, "이메일 형식이 올바르지 않습니다.")
        }
        if (birthday.isAfter(LocalDate.now())) {
            throw CoreException(ErrorType.BAD_REQUEST, "생년월일은 미래일 수 없습니다.")
        }
    }

    fun maskedName(): String {
        if (name.length <= 1) return "*"
        return name.dropLast(1) + "*"
    }

    fun changePassword(newPassword: String) {
        this.password = newPassword
    }
}
