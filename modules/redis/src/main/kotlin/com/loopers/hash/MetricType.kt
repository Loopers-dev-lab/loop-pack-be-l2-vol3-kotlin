package com.loopers.hash

enum class MetricType(val code: String) {
    VIEW("view"),
    LIKE("like"),
    ORDER("order"),
    ;

    companion object {
        fun fromCode(code: String): MetricType? = entries.firstOrNull { it.code == code }
    }
}
