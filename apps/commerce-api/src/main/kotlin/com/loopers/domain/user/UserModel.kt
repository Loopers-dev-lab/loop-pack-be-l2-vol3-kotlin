package com.loopers.domain.user

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "user")
class UserModel(
    @Convert(converter = LoginIdConverter::class)
    @Column(unique = true, nullable = false)
    var loginId: LoginId,

    @Column(nullable = false)
    var password: String,

    @Convert(converter = NameConverter::class)
    @Column(nullable = false)
    var name: Name,

    @Convert(converter = BirthDateConverter::class)
    @Column(nullable = false)
    var birthDate: BirthDate,

    @Convert(converter = EmailConverter::class)
    @Column(nullable = false)
    var email: Email,
) : BaseEntity() {

    fun updatePassword(newPassword: String) {
        password = newPassword
    }
}
