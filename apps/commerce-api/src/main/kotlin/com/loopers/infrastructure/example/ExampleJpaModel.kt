package com.loopers.infrastructure.example

import com.loopers.domain.BaseEntity
import com.loopers.domain.example.ExampleModel
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "example")
class ExampleJpaModel(
    name: String,
    description: String,
) : BaseEntity() {
    var name: String = name
        protected set

    var description: String = description
        protected set

    fun toModel(): ExampleModel = ExampleModel(
        id = id,
        name = name,
        description = description,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )

    fun updateFrom(model: ExampleModel) {
        this.name = model.name
        this.description = model.description
        if (model.deletedAt != null) {
            this.deletedAt = model.deletedAt
        }
    }

    companion object {
        fun from(model: ExampleModel): ExampleJpaModel =
            ExampleJpaModel(
                name = model.name,
                description = model.description,
            )
    }
}
