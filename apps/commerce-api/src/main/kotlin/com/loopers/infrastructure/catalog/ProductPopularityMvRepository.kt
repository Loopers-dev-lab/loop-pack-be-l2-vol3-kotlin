package com.loopers.infrastructure.catalog

import com.loopers.domain.catalog.ProductPopularityMv
import org.springframework.data.jpa.repository.JpaRepository

interface ProductPopularityMvRepository : JpaRepository<ProductPopularityMv, Long>
