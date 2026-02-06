package com.loopers.example.infrastructure

import com.loopers.example.domain.Example
import com.loopers.example.domain.ExampleRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class ExampleRepositoryImpl(
    private val exampleJpaRepository: ExampleJpaRepository,
) : ExampleRepository {
    override fun find(id: Long): Example? {
        return exampleJpaRepository.findByIdOrNull(id)?.toDomain()
    }
}
