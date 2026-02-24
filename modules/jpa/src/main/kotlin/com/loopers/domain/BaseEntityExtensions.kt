package com.loopers.domain

import java.time.ZonedDateTime

fun <T : BaseEntity> T.withBaseFields(
    id: Long,
    createdAt: ZonedDateTime? = null,
    updatedAt: ZonedDateTime? = null,
    deletedAt: ZonedDateTime? = null,
): T = this.apply {
    if (id != 0L) {
        val idField = BaseEntity::class.java.getDeclaredField("id").apply { isAccessible = true }
        idField.set(this, id)

        val createdAtField = BaseEntity::class.java.getDeclaredField("createdAt").apply { isAccessible = true }
        createdAtField.set(this, createdAt)

        val updatedAtField = BaseEntity::class.java.getDeclaredField("updatedAt").apply { isAccessible = true }
        updatedAtField.set(this, updatedAt)
    }
    deletedAt?.let {
        val deletedAtField = BaseEntity::class.java.getDeclaredField("deletedAt").apply { isAccessible = true }
        deletedAtField.set(this, it)
    }
}
