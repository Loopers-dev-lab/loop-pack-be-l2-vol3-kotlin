package com.loopers.domain.example

import java.time.ZonedDateTime

data class ExampleModel(
    val id: Long = 0,
    val name: String,
    val description: String,
    val createdAt: ZonedDateTime? = null,
    val updatedAt: ZonedDateTime? = null,
    val deletedAt: ZonedDateTime? = null,
) {
    init {
        require(name.isNotBlank()) { "이름은 비어있을 수 없습니다." }
        require(description.isNotBlank()) { "설명은 비어있을 수 없습니다." }
    }

    fun update(newDescription: String): ExampleModel {
        require(newDescription.isNotBlank()) { "설명은 비어있을 수 없습니다." }
        return copy(description = newDescription)
    }
}
