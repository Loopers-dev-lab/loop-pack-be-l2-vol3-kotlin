package com.loopers.domain.member

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDate

/**
 * 회원 엔티티
 *
 * @property loginId 로그인 ID (영문, 숫자만 허용)
 * @property password 암호화된 비밀번호
 * @property name 이름
 * @property birthDate 생년월일
 * @property email 이메일
 */
@Entity
@Table(name = "member")
class Member(
    loginId: String,
    password: String,
    name: String,
    birthDate: LocalDate,
    email: String,
) : BaseEntity() {

    @Column(name = "login_id", nullable = false, unique = true)
    var loginId: String = loginId
        protected set

    @Column(name = "password", nullable = false)
    var password: String = password
        protected set

    @Column(name = "name", nullable = false)
    var name: String = name
        protected set

    @Column(name = "birth_date", nullable = false)
    var birthDate: LocalDate = birthDate
        protected set

    @Column(name = "email", nullable = false)
    var email: String = email
        protected set

    init {
        validateLoginId(loginId)
        validateName(name)
        validateEmail(email)
    }

    private fun validateLoginId(loginId: String) {
        if (loginId.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 빈 값일 수 없습니다.")
        }
        if (!loginId.matches(LOGIN_ID_PATTERN)) {
            throw CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 영문과 숫자만 허용됩니다.")
        }
        if (loginId.length > MAX_LOGIN_ID_LENGTH) {
            throw CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 ${MAX_LOGIN_ID_LENGTH}자 이하여야 합니다.")
        }
    }

    private fun validateName(name: String) {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "이름은 빈 값일 수 없습니다.")
        }
    }

    private fun validateEmail(email: String) {
        if (!email.matches(EMAIL_PATTERN)) {
            throw CoreException(ErrorType.BAD_REQUEST, "올바른 이메일 형식이 아닙니다.")
        }
    }

    /**
     * 이름 마스킹: 마지막 글자를 *로 대체
     * - "홍길동" -> "홍길*"
     * - "홍" -> "*"
     */
    fun getMaskedName(): String {
        return if (name.length <= 1) {
            MASK_CHAR
        } else {
            name.dropLast(1) + MASK_CHAR
        }
    }

    fun changePassword(newEncodedPassword: String) {
        this.password = newEncodedPassword
    }

    /**
     * 📌 Kotlin 설명: companion object
     * - Java의 static 멤버와 유사
     * - 클래스 레벨에서 공유되는 상수나 팩토리 메서드 정의에 사용
     */
    companion object {
        private val LOGIN_ID_PATTERN = Regex("^[a-zA-Z0-9]+$")
        private val EMAIL_PATTERN = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        private const val MAX_LOGIN_ID_LENGTH = 10
        private const val MASK_CHAR = "*"
    }
}
