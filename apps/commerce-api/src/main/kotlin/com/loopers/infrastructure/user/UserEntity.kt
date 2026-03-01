package com.loopers.infrastructure.user

import com.loopers.infrastructure.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDate

@Table(name = "users")
@Entity
class UserEntity(
    id: Long? = null,
    @Column(nullable = false, unique = true)
    val loginId: String,
    @Column(nullable = false)
    val password: String,
    @Column(nullable = false)
    val name: String,
    @Column(nullable = false)
    val birthDate: LocalDate,
    @Column(nullable = false)
    val email: String,
) : BaseEntity() {

    init {
        this.id = id
    }
}
