package com.loopers.application.example

import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType
import com.loopers.domain.example.ExampleModel
import com.loopers.domain.example.ExampleRepository
import org.springframework.stereotype.Component

@Component
class ExampleService(
    private val exampleRepository: ExampleRepository,
) {
    fun getExample(id: Long): ExampleModel {
        return exampleRepository.find(id)
            ?: throw CoreException(errorType = ErrorType.NOT_FOUND, customMessage = "[id = $id] 예시를 찾을 수 없습니다.")
    }
}
