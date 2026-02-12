package com.loopers.application.example

import com.loopers.domain.example.ExampleModel

data class ExampleInfo(
    val id: Long,
    val name: String,
    val description: String,
) {
    companion object {
        fun from(model: ExampleModel): ExampleInfo {
            val id = requireNotNull(model.id) {
                "ExampleModel.id가 null입니다. 저장 후 매핑해야 합니다."
            }
            return ExampleInfo(
                id = id,
                name = model.name,
                description = model.description,
            )
        }
    }
}
