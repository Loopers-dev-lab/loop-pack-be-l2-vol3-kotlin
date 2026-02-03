package com.loopers.domain.example

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "users")
class User(
    loginId: String,
    password: String,
    name: String,
    birthday: LocalDate,
    email: String,
) : BaseEntity() {

    var loginId: String = loginId
        protected set

    var password: String = password
        protected set

    var name: String = name
        protected set

    var birthday: LocalDate = birthday
        protected set

    var email: String = email
        protected set

    init {
        if (loginId.isBlank()) throw CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 비어있을 수 없습니다.")

        val loginIdPattern = Regex(".*([가-힣]|[!@#$%^&*(),.?\":{}|<>]).*")
        if (loginIdPattern.containsMatchIn(loginId)) throw CoreException(ErrorType.BAD_REQUEST, "로그인 ID를 확인 해주세요.")

        val passwordPattern = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&#]{8,16}\$")
        if (!passwordPattern.containsMatchIn(password)) throw CoreException(ErrorType.BAD_REQUEST, "비밀번호를 올바르게 입력해주세요.")

        val birthdayString = birthday.toString().replace("-", "")
        if (password.contains(birthdayString)) throw CoreException(ErrorType.BAD_REQUEST, "비밀번호에 생년월일을 포함할 수 없습니다.")

        if (name.isBlank()) throw CoreException(ErrorType.BAD_REQUEST, "이름은 비어있을 수 없습니다.")

        val emailPattern = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")
        if (!emailPattern.containsMatchIn(email)) throw CoreException(ErrorType.BAD_REQUEST, "이메일을 확인 해주세요.")
    }
}
