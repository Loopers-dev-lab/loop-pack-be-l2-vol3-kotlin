package com.loopers.domain.brand

import com.loopers.domain.brand.vo.BrandName
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

class Brand(
    val id: Long? = null,
    name: BrandName,
    status: BrandStatus = BrandStatus.ACTIVE,
) {
    var name: BrandName = name
        private set

    var status: BrandStatus = status
        private set

    fun changeName(newName: BrandName) {
        this.name = newName
    }

    fun deactivate() {
        if (status == BrandStatus.INACTIVE) {
            throw CoreException(ErrorType.BRAND_ALREADY_INACTIVE)
        }
        this.status = BrandStatus.INACTIVE
    }
}
