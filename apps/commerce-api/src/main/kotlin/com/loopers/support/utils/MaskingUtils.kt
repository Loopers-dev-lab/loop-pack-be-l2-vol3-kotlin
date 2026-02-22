package com.loopers.support.utils

object MaskingUtils {

    fun maskLastCharacter(value: String): String {
        if (value.length <= 1) return "*"
        return value.dropLast(1) + "*"
    }
}
