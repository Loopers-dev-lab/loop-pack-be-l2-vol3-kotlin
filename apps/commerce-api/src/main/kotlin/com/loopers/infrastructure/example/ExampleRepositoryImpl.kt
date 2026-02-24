package com.loopers.infrastructure.example

import com.loopers.domain.example.Example
import com.loopers.domain.example.ExampleRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class ExampleRepositoryImpl(
    private val exampleJpaRepository: ExampleJpaRepository,
) : ExampleRepository {
    override fun find(id: Long): Example? {
        return exampleJpaRepository.findByIdOrNull(id)?.toDomain()
    }
}
