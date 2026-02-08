package com.loopers.example.application

import org.springframework.stereotype.Component

// Facade는 Service 오케스트레이션 역할만 맡고 비즈니스 규칙은 넣지 않는다. (조합/변환만)
// Controller와 Service 사이에서 여러 유스케이스(Application Service)를 조합해 화면/클라이언트 전용 응답을 구성할 때 사용한다.
// 단일 유스케이스면 Facade 없이 Service 직접 호출한다.
@Component
class ExampleFacade(
    private val exampleService: ExampleService,
    private val sampleService: SampleService,
) {
    fun getExample(id: Long): ExampleInfo {
        return exampleService.getExample(id)
    }
}
