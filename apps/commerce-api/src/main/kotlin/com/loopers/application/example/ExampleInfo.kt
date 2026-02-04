package com.loopers.application.example

import com.loopers.domain.example.ExampleModel

data class ExampleInfo(
    val id: Long,
    val name: String,
    val description: String,
) {
    companion object {
        /**
         * Create an ExampleInfo from an ExampleModel.
         *
         * The resulting ExampleInfo copies `name` and `description` from `model` and requires `model.id` to be non-null.
         *
         * @param model The source ExampleModel to convert.
         * @return An ExampleInfo constructed from the given model's properties.
         * @throws NullPointerException if `model.id` is null.
         */
        fun from(model: ExampleModel): ExampleInfo {
            return ExampleInfo(
                id = model.id!!,
                name = model.name,
                description = model.description,
            )
        }
    }
}