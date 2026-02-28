package com.loopers.domain.brand

import java.time.ZonedDateTime

class Brand private constructor(
    val persistenceId: Long?,
    val name: BrandName,
    val description: String?,
    val logoUrl: String?,
    val status: BrandStatus,
    val deletedAt: ZonedDateTime?,
) {

    fun assertNotDeleted() {
        if (isDeleted()) {
            throw BrandException(BrandError.DELETED, "삭제된 브랜드입니다.")
        }
    }

    fun update(name: BrandName, description: String?, logoUrl: String?): Brand {
        assertNotDeleted()
        return Brand(
            persistenceId = persistenceId,
            name = name,
            description = description,
            logoUrl = logoUrl,
            status = status,
            deletedAt = deletedAt,
        )
    }

    fun activate(): Brand {
        return Brand(
            persistenceId = persistenceId,
            name = name,
            description = description,
            logoUrl = logoUrl,
            status = BrandStatus.ACTIVE,
            deletedAt = deletedAt,
        )
    }

    fun deactivate(): Brand {
        return Brand(
            persistenceId = persistenceId,
            name = name,
            description = description,
            logoUrl = logoUrl,
            status = BrandStatus.INACTIVE,
            deletedAt = deletedAt,
        )
    }

    fun delete(): Brand {
        return Brand(
            persistenceId = persistenceId,
            name = name,
            description = description,
            logoUrl = logoUrl,
            status = status,
            deletedAt = ZonedDateTime.now(),
        )
    }

    fun isDeleted(): Boolean = deletedAt != null

    companion object {
        fun create(name: BrandName, description: String?, logoUrl: String?): Brand {
            return Brand(
                persistenceId = null,
                name = name,
                description = description,
                logoUrl = logoUrl,
                status = BrandStatus.ACTIVE,
                deletedAt = null,
            )
        }

        fun reconstitute(
            persistenceId: Long,
            name: BrandName,
            description: String?,
            logoUrl: String?,
            status: BrandStatus,
            deletedAt: ZonedDateTime?,
        ): Brand {
            return Brand(
                persistenceId = persistenceId,
                name = name,
                description = description,
                logoUrl = logoUrl,
                status = status,
                deletedAt = deletedAt,
            )
        }
    }
}
