package com.loopers.infrastructure.lock

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "products")
class ProductLockEntity(
    @Id
    val id: Long,
)
