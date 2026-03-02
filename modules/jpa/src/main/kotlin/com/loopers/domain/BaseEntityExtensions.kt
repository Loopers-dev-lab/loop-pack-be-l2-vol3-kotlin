package com.loopers.domain

import java.time.ZonedDateTime

private val idField = BaseEntity::class.java.getDeclaredField("id").apply { isAccessible = true }
private val createdAtField = BaseEntity::class.java.getDeclaredField("createdAt").apply { isAccessible = true }
private val updatedAtField = BaseEntity::class.java.getDeclaredField("updatedAt").apply { isAccessible = true }
private val deletedAtField = BaseEntity::class.java.getDeclaredField("deletedAt").apply { isAccessible = true }

fun <T : BaseEntity> T.withBaseFields(
    id: Long,
    createdAt: ZonedDateTime? = null,
    updatedAt: ZonedDateTime? = null,
    deletedAt: ZonedDateTime? = null,
): T = this.apply {
    if (id != 0L) {
        idField.set(this, id)
        val now = ZonedDateTime.now()
        createdAtField.set(this, createdAt ?: now)
        updatedAtField.set(this, updatedAt ?: now)
    }
    deletedAt?.let { deletedAtField.set(this, it) }
}
