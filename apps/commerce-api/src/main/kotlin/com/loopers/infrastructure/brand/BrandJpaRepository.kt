package com.loopers.infrastructure.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface BrandJpaRepository : JpaRepository<Brand, Long>, BrandRepository {

    @Query("SELECT b FROM Brand b WHERE b.id = :id")
    override fun findByIdOrNull(@Param("id") id: Long): Brand?

    @Query("SELECT b FROM Brand b WHERE b.id IN :ids")
    override fun findAllByIds(@Param("ids") ids: List<Long>): List<Brand>

    @Query("SELECT b FROM Brand b WHERE b.deletedAt IS NULL")
    override fun findAllActive(): List<Brand>

    @Query(
        "SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END " +
            "FROM Brand b WHERE b.name = :name AND b.deletedAt IS NULL",
    )
    override fun existsActiveByName(@Param("name") name: String): Boolean
}
