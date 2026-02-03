package com.loopers.infrastructure.user

import com.loopers.domain.BaseEntity
import com.loopers.domain.user.GenderType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "users")
class UserEntity(

    @Column(name = "login_id", nullable = false, unique = true, length = 10)
    val loginId: String,

    @Column(name = "password", nullable = false)
    val password: String,

    @Column(name = "name", nullable = false)
    val name: String,

    @Column(name = "birth_date", nullable = false)
    val birthDate: LocalDate,

    @Column(name = "email", nullable = false)
    val email: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    val gender: GenderType,
) : BaseEntity()
