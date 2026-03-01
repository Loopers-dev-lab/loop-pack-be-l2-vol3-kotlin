package com.loopers.domain.brand

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

class Brand private constructor(
    val id: Long?,
    val name: BrandName,
    val status: Status,
) {
    enum class Status {
        ACTIVE,
        INACTIVE,
    }

    fun update(name: String, status: String): Brand {
        val newStatus = Status.entries.find { it.name == status }
            ?: throw CoreException(ErrorType.BRAND_INVALID_STATUS)
        return copy(name = BrandName(name), status = newStatus)
    }

    private fun copy(
        id: Long? = this.id,
        name: BrandName = this.name,
        status: Status = this.status,
    ): Brand = Brand(id, name, status)

    companion object {
        fun register(name: String): Brand {
            return Brand(
                id = null,
                name = BrandName(name),
                status = Status.INACTIVE,
            )
        }

        fun retrieve(
            id: Long,
            name: String,
            status: Status,
        ): Brand {
            return Brand(
                id = id,
                name = BrandName(name),
                status = status,
            )
        }
    }
}
