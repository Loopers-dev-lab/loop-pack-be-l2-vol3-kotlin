package com.loopers.example.domain

class Example private constructor(
    val id: Long,
    val name: String,
    val description: String,
) {

    companion object {
        fun register(id: Long, name: String, description: String): Example {
            // 검증
            return Example(id, name, description)
        }

        fun retrieve(id: Long, name: String, description: String): Example {
            return Example(id, name, description)
        }
    }
}
