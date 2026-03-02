package com.loopers.infrastructure.brand

import com.loopers.domain.brand.BrandModel
import com.loopers.domain.brand.BusinessNumber
import com.loopers.domain.brand.Name
import org.springframework.data.jpa.repository.JpaRepository

interface BrandJpaRepository : JpaRepository<BrandModel, Long> {
    fun existsByName(name: Name): Boolean
    fun existsByBusinessNumber(businessNumber: BusinessNumber): Boolean
}
