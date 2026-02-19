package com.loopers.infrastructure.catalog.brand

import com.loopers.domain.catalog.brand.Brand
import org.springframework.data.jpa.repository.JpaRepository

interface BrandJpaRepository : JpaRepository<Brand, Long>
