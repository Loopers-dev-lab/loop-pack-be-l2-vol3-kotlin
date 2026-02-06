package com.loopers.example.application

import com.loopers.example.domain.ExampleRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Service
class ExampleService(
    private val exampleRepository: ExampleRepository,
) {
    fun getExample(id: Long): ExampleInfo {
        val example = exampleRepository.find(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "[id = $id] 예시를 찾을 수 없습니다.")
        return ExampleInfo.from(example)
    }
}
