package com.loopers.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass

@MappedSuperclass
abstract class AdminAuditEntity : BaseEntity() {
    @Column(name = "created_by", nullable = false, updatable = false)
    lateinit var createdBy: String
        protected set

    @Column(name = "updated_by", nullable = false)
    lateinit var updatedBy: String
        protected set

    @Column(name = "deleted_by")
    var deletedBy: String? = null
        protected set

    fun updateBy(admin: String) {
        updatedBy = admin
    }

    fun deleteBy(admin: String) {
        delete()
        deletedBy = deletedBy ?: admin
    }
}
