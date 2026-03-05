package com.loopers.application.example

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ExampleFacade(
    private val exampleService: ExampleService,
) {
    @Transactional(readOnly = true)
    fun getExample(id: Long): ExampleInfo {
        return exampleService.getExample(id)
            .let { ExampleInfo.from(it) }
    }
}
