package com.loopers.domain.user

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "tb_user")
class UserModel(
    loginId: LoginId,
    encryptedPassword: String,
    name: Name,
    birthDate: BirthDate,
    email: Email,
) : BaseEntity() {

    @Column(unique = true, nullable = false)
    var loginId: LoginId = loginId
        protected set

    @Column(nullable = false)
    var password: String = encryptedPassword
        protected set

    @Column(nullable = false)
    var name: Name = name
        protected set

    @Column(nullable = false)
    var birthDate: BirthDate = birthDate
        protected set

    @Column(nullable = false)
    var email: Email = email
        protected set

    fun updatePassword(newEncryptedPassword: String) {
        password = newEncryptedPassword
    }
}
