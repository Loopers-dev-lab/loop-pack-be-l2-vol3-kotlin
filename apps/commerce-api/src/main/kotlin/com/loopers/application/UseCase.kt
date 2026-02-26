package com.loopers.application

interface UseCase<IN, OUT> {
    fun execute(criteria: IN): OUT
}
