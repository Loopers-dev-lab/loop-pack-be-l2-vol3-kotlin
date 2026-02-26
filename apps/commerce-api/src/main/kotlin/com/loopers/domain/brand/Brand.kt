package com.loopers.domain.brand

import com.loopers.domain.BaseEntity
import com.loopers.support.error.BrandException
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "brands")
class Brand private constructor(
    name: String,
    deletedAt: ZonedDateTime? = null,
) : BaseEntity() {

    @Column(nullable = false, length = 50)
    var name: String = name
        protected set

    @Column(name = "deleted_at")
    var deletedAt: ZonedDateTime? = deletedAt
        protected set

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
            return Brand(name = name)
        }

        private fun validateName(name: String) {
            val trimmed = name.trim()
            if (trimmed.length < NAME_MIN_LENGTH || trimmed.length > NAME_MAX_LENGTH) {
                throw BrandException.invalidName()
            }
        }
    }
}
