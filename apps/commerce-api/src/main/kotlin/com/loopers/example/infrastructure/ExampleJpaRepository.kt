package com.loopers.example.infrastructure

import org.springframework.data.jpa.repository.JpaRepository

interface ExampleJpaRepository : JpaRepository<ExampleEntity, Long>
