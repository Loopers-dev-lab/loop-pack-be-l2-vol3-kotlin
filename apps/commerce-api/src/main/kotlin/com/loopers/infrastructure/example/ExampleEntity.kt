package com.loopers.infrastructure.example

import com.loopers.domain.BaseEntity
import com.loopers.domain.example.Example
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Table(name = "example")
@Entity
class ExampleEntity(
    id: Long? = null,
    val name: String,
    val description: String,
) : BaseEntity() {

    init {
        this.id = id
        if (name.isBlank()) throw CoreException(ErrorType.BAD_REQUEST, "이름은 필수값입니다.")
        if (description.isBlank()) throw CoreException(ErrorType.BAD_REQUEST, "설명은 필수값입니다.")
    }

    fun toDomain(): Example {
        return Example.retrieve(id!!, name, description)
    }

    fun toEntity(domain: Example): ExampleEntity {
        return ExampleEntity(id = domain.id, name = domain.name, description = domain.description)
    }
}
