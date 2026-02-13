package com.loopers.domain.example

import com.loopers.support.error.CommonErrorCode
import com.loopers.support.error.CoreException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ExampleService(
    private val exampleRepository: ExampleRepository,
) {
    @Transactional(readOnly = true)
    fun getExample(id: Long): ExampleModel {
        return exampleRepository.find(id)
            ?: throw CoreException(CommonErrorCode.RESOURCE_NOT_FOUND, "[id = $id] 예시를 찾을 수 없습니다.")
    }
}
