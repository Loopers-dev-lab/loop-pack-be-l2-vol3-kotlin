package com.loopers.infrastructure.cache

import org.aspectj.lang.JoinPoint
import org.aspectj.lang.reflect.MethodSignature

/**
 * 캐시 키 리졸버.
 *
 * - key 비어있음 → 메서드 파라미터 자동 조합
 * - key 있음 → {paramName} 또는 {paramName.property} 템플릿을 실제 값으로 치환
 *
 * 예: "" + args=[42] → "42"  (자동 파생)
 *     "{productId}" + args=[42] → "42"  (단순 템플릿)
 *     "{pageable.pageNumber}:{pageable.pageSize}:{brandId}" → "0:20:5"  (중첩 프로퍼티)
 */
object CacheKeyResolver {

    private val PLACEHOLDER_REGEX = Regex("\\{([^}]+)}")

    fun resolve(template: String, joinPoint: JoinPoint): String {
        if (template.isBlank()) return deriveFromParams(joinPoint)

        val signature = joinPoint.signature as MethodSignature
        val paramNames = signature.parameterNames ?: return template
        val args = joinPoint.args
        val paramMap = paramNames.mapIndexed { index, name -> name to args[index] }.toMap()

        return PLACEHOLDER_REGEX.replace(template) { matchResult ->
            resolveExpression(matchResult.groupValues[1], paramMap)
        }
    }

    /**
     * 표현식을 값으로 변환.
     *
     * - 단순 파라미터: "brandId" → paramMap["brandId"].toString()
     * - 중첩 프로퍼티: "pageable.pageNumber" → paramMap["pageable"].getPageNumber().toString()
     */
    private fun resolveExpression(expr: String, paramMap: Map<String, Any?>): String {
        val parts = expr.split(".")
        val paramName = parts[0]
        var value: Any? = paramMap[paramName]

        for (i in 1 until parts.size) {
            if (value == null) return "null"
            value = getProperty(value, parts[i])
        }

        return value?.toString() ?: "null"
    }

    /**
     * Java getter 컨벤션으로 프로퍼티 값 추출.
     *
     * getXxx() → isXxx() → 필드 직접 접근 순으로 시도.
     */
    private fun getProperty(obj: Any, propertyName: String): Any? {
        val capitalizedName = propertyName.replaceFirstChar { it.uppercase() }

        // 1. getXxx()
        try {
            return obj::class.java.getMethod("get$capitalizedName").invoke(obj)
        } catch (_: NoSuchMethodException) {
            // fallthrough
        }

        // 2. isXxx() (boolean)
        try {
            return obj::class.java.getMethod("is$capitalizedName").invoke(obj)
        } catch (_: NoSuchMethodException) {
            // fallthrough
        }

        // 3. 필드 직접 접근
        return try {
            val field = obj::class.java.getDeclaredField(propertyName)
            field.isAccessible = true
            field.get(obj)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 메서드 파라미터 값을 ":"으로 조합하여 캐시 키 자동 생성.
     *
     * - 파라미터 없음 → "_"
     * - 파라미터 1개 → "42"
     * - 파라미터 2개+ → "42:3"
     */
    private fun deriveFromParams(joinPoint: JoinPoint): String {
        val args = joinPoint.args
        if (args.isEmpty()) return "_"
        return args.joinToString(":") { it?.toString() ?: "null" }
    }
}
