package com.loopers.domain.brand

import com.loopers.support.error.BrandException
import java.time.ZonedDateTime

class Brand private constructor(
    val id: Long,
    name: String,
    deletedAt: ZonedDateTime?,
    val createdAt: ZonedDateTime?,
    val updatedAt: ZonedDateTime?,
) {

    var name: String = name
        private set

    var deletedAt: ZonedDateTime? = deletedAt
        private set

    fun isDeleted(): Boolean = deletedAt != null

    fun delete() {
        this.deletedAt = ZonedDateTime.now()
    }

    fun update(name: String) {
        validateName(name)
        this.name = name
    }

    companion object {
        private const val NAME_MIN_LENGTH = 1
        private const val NAME_MAX_LENGTH = 50

        fun create(name: String): Brand {
            validateName(name)
            return Brand(
                id = 0,
                name = name,
                deletedAt = null,
                createdAt = null,
                updatedAt = null,
            )
        }

        fun reconstruct(
            id: Long,
            name: String,
            deletedAt: ZonedDateTime?,
            createdAt: ZonedDateTime,
            updatedAt: ZonedDateTime,
        ): Brand {
            return Brand(
                id = id,
                name = name,
                deletedAt = deletedAt,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )
        }

        private fun validateName(name: String) {
            val trimmed = name.trim()
            if (trimmed.length < NAME_MIN_LENGTH || trimmed.length > NAME_MAX_LENGTH) {
                throw BrandException.invalidName()
            }
        }
    }
}
