package com.loopers.infrastructure.example

import org.springframework.data.jpa.repository.JpaRepository

interface ExampleJpaRepository : JpaRepository<ExampleJpaModel, Long>
