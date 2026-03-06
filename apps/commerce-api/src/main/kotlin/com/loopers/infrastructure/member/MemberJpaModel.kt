package com.loopers.infrastructure.member

import com.loopers.domain.BaseEntity
import com.loopers.domain.member.MemberModel
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "member")
class MemberJpaModel(
    loginId: String,
    password: String,
    name: String,
    birthday: LocalDate,
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

    @Column(name = "birthday", nullable = false)
    var birthday: LocalDate = birthday
        protected set

    @Column(name = "email", nullable = false, unique = true)
    var email: String = email
        protected set

    fun toModel(): MemberModel = MemberModel(
        id = id,
        loginId = loginId,
        password = password,
        name = name,
        birthday = birthday,
        email = email,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )

    fun updateFrom(model: MemberModel) {
        this.password = model.password
        this.name = model.name
        this.email = model.email
        if (model.deletedAt != null) {
            this.deletedAt = model.deletedAt
        }
    }

    companion object {
        fun from(model: MemberModel): MemberJpaModel =
            MemberJpaModel(
                loginId = model.loginId,
                password = model.password,
                name = model.name,
                birthday = model.birthday,
                email = model.email,
            )
    }
}
