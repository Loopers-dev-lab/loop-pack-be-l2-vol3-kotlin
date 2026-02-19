package com.loopers.infrastructure.catalog.brand

import com.loopers.domain.catalog.brand.entity.Brand
import org.springframework.data.jpa.repository.JpaRepository

interface BrandJpaRepository : JpaRepository<Brand, Long>
