package com.loopers.infrastructure.cache

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.SerializationException

/**
 * 리턴 타입 기반 Redis 직렬화기.
 *
 * GenericJackson2JsonRedisSerializer + activateDefaultTyping(EVERYTHING) 방식은
 * JSON에 "@class" 메타데이터를 모든 값에 삽입한다. 이는:
 * - 보안 위험: allowIfBaseType(Any) → 임의 클래스 인스턴스화 가능
 * - 클래스 리네임 시 기존 캐시 역직렬화 실패
 * - 페이로드 비대: 실 데이터보다 메타데이터가 클 수 있음
 *
 * 이 직렬화기는 "@class" 없이 순수 JSON을 저장하고,
 * [CachedAspect]가 ThreadLocal로 전달한 리턴 타입을 기반으로 역직렬화한다.
 *
 * 직렬화: 순수 JSON (타입 메타 없음)
 * 역직렬화: ThreadLocal<JavaType> → objectMapper.readValue(bytes, type)
 */
class TypeAwareRedisSerializer(
    private val objectMapper: ObjectMapper,
) : RedisSerializer<Any> {

    companion object {
        private val expectedType = ThreadLocal<JavaType>()

        /** 역직렬화 전에 호출 — 대상 타입 설정 */
        fun setExpectedType(type: JavaType) {
            expectedType.set(type)
        }

        /** 역직렬화 후 finally에서 호출 — ThreadLocal 정리 */
        fun clearExpectedType() {
            expectedType.remove()
        }
    }

    override fun serialize(value: Any?): ByteArray? {
        if (value == null) return null
        return try {
            objectMapper.writeValueAsBytes(value)
        } catch (e: Exception) {
            throw SerializationException("캐시 직렬화 실패", e)
        }
    }

    override fun deserialize(bytes: ByteArray?): Any? {
        if (bytes == null || bytes.isEmpty()) return null
        return try {
            val type = expectedType.get()
            if (type != null && Page::class.java.isAssignableFrom(type.rawClass)) {
                deserializePage(bytes, type)
            } else if (type != null) {
                objectMapper.readValue(bytes, type)
            } else {
                // ThreadLocal 미설정 시 fallback — LinkedHashMap으로 역직렬화됨
                objectMapper.readValue(bytes, Any::class.java)
            }
        } catch (e: Exception) {
            throw SerializationException("캐시 역직렬화 실패", e)
        }
    }

    /**
     * Page<T> 역직렬화.
     *
     * PageModule은 직렬화 시 Page를 PagedModel JSON으로 변환한다:
     * ```json
     * { "content": [...], "page": { "size": 20, "number": 0, "totalElements": 100, "totalPages": 5 } }
     * ```
     * 하지만 역직렬화기는 제공하지 않는다 (Page는 인터페이스).
     *
     * 이 메서드에서 PagedModel JSON → PageImpl 수동 복원.
     * Sort 정보는 캐시 키에 이미 포함되어 있으므로 생략해도 무방.
     */
    private fun deserializePage(bytes: ByteArray, pageType: JavaType): Page<*> {
        val tree = objectMapper.readTree(bytes)

        val contentType = pageType.containedType(0)
        val listType = objectMapper.typeFactory.constructCollectionType(List::class.java, contentType)
        val content: List<Any> = objectMapper.readValue(
            objectMapper.treeAsTokens(tree.get("content")),
            listType,
        )

        val pageNode = tree.get("page")
        val number = pageNode.get("number").asInt()
        val size = pageNode.get("size").asInt()
        val totalElements = pageNode.get("totalElements").asLong()

        return PageImpl(content, PageRequest.of(number, size), totalElements)
    }
}
