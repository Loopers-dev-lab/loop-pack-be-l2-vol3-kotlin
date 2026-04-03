package com.loopers.config.kafka

import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.springframework.stereotype.Component

@Component
class AvroSchemaProvider {
    private val schemas: Map<String, Schema> = mapOf(
        KafkaTopics.CATALOG_EVENTS to loadSchema("avro/catalog-event.avsc"),
        KafkaTopics.ORDER_EVENTS to loadSchema("avro/order-event.avsc"),
        KafkaTopics.COUPON_ISSUE_REQUESTS to loadSchema("avro/coupon-issue-request.avsc"),
    )

    fun getSchema(topic: String): Schema =
        schemas[topic] ?: throw IllegalArgumentException("등록되지 않은 토픽입니다: $topic")

    fun toGenericRecord(topic: String, payload: Map<String, Any?>): GenericRecord {
        val schema = getSchema(topic)
        val record = GenericData.Record(schema)
        for (field in schema.fields) {
            val value = payload[field.name()]
            record.put(field.name(), convertValue(value, field.schema()))
        }
        return record
    }

    @Suppress("UNCHECKED_CAST")
    private fun convertValue(value: Any?, schema: Schema): Any? {
        if (value == null) return null
        return when (schema.type) {
            Schema.Type.UNION -> {
                val nonNullSchema = schema.types.first { it.type != Schema.Type.NULL }
                convertValue(value, nonNullSchema)
            }
            Schema.Type.LONG -> (value as Number).toLong()
            Schema.Type.INT -> (value as Number).toInt()
            Schema.Type.STRING -> value.toString()
            Schema.Type.ARRAY -> {
                val itemSchema = schema.elementType
                val list = value as List<Any?>
                GenericData.Array(schema, list.map { convertValue(it, itemSchema) })
            }
            Schema.Type.RECORD -> {
                val map = value as Map<String, Any?>
                val record = GenericData.Record(schema)
                for (field in schema.fields) {
                    record.put(field.name(), convertValue(map[field.name()], field.schema()))
                }
                record
            }
            else -> value
        }
    }

    private fun loadSchema(resourcePath: String): Schema {
        val stream = javaClass.classLoader.getResourceAsStream(resourcePath)
            ?: throw IllegalStateException("Avro 스키마를 찾을 수 없습니다: $resourcePath")
        return Schema.Parser().parse(stream)
    }
}
