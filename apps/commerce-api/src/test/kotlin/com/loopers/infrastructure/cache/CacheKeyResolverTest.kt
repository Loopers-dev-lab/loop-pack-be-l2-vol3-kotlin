package com.loopers.infrastructure.cache

import org.aspectj.lang.JoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

class CacheKeyResolverTest {

    @Nested
    @DisplayName("템플릿 키 치환")
    inner class TemplateResolution {

        @Test
        @DisplayName("단일 플레이스홀더를 파라미터 값으로 치환한다")
        fun resolveSinglePlaceholder() {
            // Arrange
            val joinPoint = mockJoinPoint(
                paramNames = arrayOf("productId"),
                args = arrayOf(42L),
            )

            // Act
            val result = CacheKeyResolver.resolve("{productId}", joinPoint)

            // Assert
            assertEquals("42", result)
        }

        @Test
        @DisplayName("복수 플레이스홀더를 각각의 파라미터 값으로 치환한다")
        fun resolveMultiplePlaceholders() {
            // Arrange
            val joinPoint = mockJoinPoint(
                paramNames = arrayOf("brandId", "page"),
                args = arrayOf(3L, 0),
            )

            // Act
            val result = CacheKeyResolver.resolve("{brandId}:{page}", joinPoint)

            // Assert
            assertEquals("3:0", result)
        }

        @Test
        @DisplayName("플레이스홀더가 없으면 템플릿을 그대로 반환한다")
        fun resolveNoPlaceholder() {
            // Arrange
            val joinPoint = mockJoinPoint(
                paramNames = arrayOf("productId"),
                args = arrayOf(42L),
            )

            // Act
            val result = CacheKeyResolver.resolve("static-key", joinPoint)

            // Assert
            assertEquals("static-key", result)
        }

        @Test
        @DisplayName("null 파라미터는 'null' 문자열로 치환한다")
        fun resolveNullParameter() {
            // Arrange
            val joinPoint = mockJoinPoint(
                paramNames = arrayOf("brandId"),
                args = arrayOf(null),
            )

            // Act
            val result = CacheKeyResolver.resolve("{brandId}", joinPoint)

            // Assert
            assertEquals("null", result)
        }

        @Test
        @DisplayName("parameterNames가 null이면 템플릿을 그대로 반환한다")
        fun resolveNullParamNames() {
            // Arrange
            val joinPoint = mockJoinPoint(
                paramNames = null,
                args = arrayOf(42L),
            )

            // Act
            val result = CacheKeyResolver.resolve("{productId}", joinPoint)

            // Assert
            assertEquals("{productId}", result)
        }
    }

    @Nested
    @DisplayName("자동 키 파생 (key 생략)")
    inner class AutoDerive {

        @Test
        @DisplayName("key가 빈 문자열이면 파라미터 값을 자동 조합한다")
        fun deriveFromSingleParam() {
            // Arrange
            val joinPoint = mockJoinPoint(
                paramNames = arrayOf("productId"),
                args = arrayOf(42L),
            )

            // Act
            val result = CacheKeyResolver.resolve("", joinPoint)

            // Assert
            assertEquals("42", result)
        }

        @Test
        @DisplayName("복수 파라미터는 ':'으로 조합한다")
        fun deriveFromMultipleParams() {
            // Arrange
            val joinPoint = mockJoinPoint(
                paramNames = arrayOf("brandId", "page"),
                args = arrayOf(3L, 0),
            )

            // Act
            val result = CacheKeyResolver.resolve("", joinPoint)

            // Assert
            assertEquals("3:0", result)
        }

        @Test
        @DisplayName("파라미터가 없으면 '_'를 반환한다")
        fun deriveFromNoParams() {
            // Arrange
            val joinPoint = mockJoinPoint(
                paramNames = arrayOf(),
                args = arrayOf(),
            )

            // Act
            val result = CacheKeyResolver.resolve("", joinPoint)

            // Assert
            assertEquals("_", result)
        }

        @Test
        @DisplayName("null 파라미터는 'null' 문자열로 변환한다")
        fun deriveFromNullParam() {
            // Arrange
            val joinPoint = mockJoinPoint(
                paramNames = arrayOf("brandId"),
                args = arrayOf(null),
            )

            // Act
            val result = CacheKeyResolver.resolve("", joinPoint)

            // Assert
            assertEquals("null", result)
        }
    }

    @Nested
    @DisplayName("중첩 프로퍼티 접근")
    inner class NestedPropertyAccess {

        @Test
        @DisplayName("단일 중첩 프로퍼티를 getter로 추출한다")
        fun resolveNestedProperty() {
            // Arrange
            val dto = SampleDto(42L, "test")
            val joinPoint = mockJoinPoint(
                paramNames = arrayOf("dto"),
                args = arrayOf(dto),
            )

            // Act
            val result = CacheKeyResolver.resolve("{dto.id}", joinPoint)

            // Assert
            assertEquals("42", result)
        }

        @Test
        @DisplayName("Pageable의 중첩 프로퍼티로 캐시 키를 생성한다")
        fun resolvePageableNestedProperties() {
            // Arrange
            val pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending())
            val joinPoint = mockJoinPoint(
                paramNames = arrayOf("pageable", "brandId"),
                args = arrayOf(pageable, 5L),
            )

            // Act
            val result = CacheKeyResolver.resolve(
                "{pageable.pageNumber}:{pageable.pageSize}:{brandId}",
                joinPoint,
            )

            // Assert
            assertEquals("0:20:5", result)
        }

        @Test
        @DisplayName("중첩 프로퍼티와 단순 파라미터를 혼합하여 키를 생성한다")
        fun resolveMixedNestedAndSimple() {
            // Arrange
            val dto = SampleDto(7L, "product")
            val joinPoint = mockJoinPoint(
                paramNames = arrayOf("dto", "page"),
                args = arrayOf(dto, 3),
            )

            // Act
            val result = CacheKeyResolver.resolve("{dto.id}:{dto.name}:{page}", joinPoint)

            // Assert
            assertEquals("7:product:3", result)
        }

        @Test
        @DisplayName("null 객체의 프로퍼티는 'null'로 변환한다")
        fun resolveNullObjectProperty() {
            // Arrange
            val joinPoint = mockJoinPoint(
                paramNames = arrayOf("dto"),
                args = arrayOf(null),
            )

            // Act
            val result = CacheKeyResolver.resolve("{dto.id}", joinPoint)

            // Assert
            assertEquals("null", result)
        }

        @Test
        @DisplayName("존재하지 않는 프로퍼티는 'null'로 변환한다")
        fun resolveNonExistentProperty() {
            // Arrange
            val dto = SampleDto(42L, "test")
            val joinPoint = mockJoinPoint(
                paramNames = arrayOf("dto"),
                args = arrayOf(dto),
            )

            // Act
            val result = CacheKeyResolver.resolve("{dto.nonExistent}", joinPoint)

            // Assert
            assertEquals("null", result)
        }
    }

    private fun mockJoinPoint(
        paramNames: Array<String>?,
        args: Array<Any?>,
    ): JoinPoint {
        val signature = mock<MethodSignature>()
        whenever(signature.parameterNames).thenReturn(paramNames)

        val joinPoint = mock<JoinPoint>()
        whenever(joinPoint.signature).thenReturn(signature)
        whenever(joinPoint.args).thenReturn(args)

        return joinPoint
    }

    private data class SampleDto(val id: Long, val name: String)
}
