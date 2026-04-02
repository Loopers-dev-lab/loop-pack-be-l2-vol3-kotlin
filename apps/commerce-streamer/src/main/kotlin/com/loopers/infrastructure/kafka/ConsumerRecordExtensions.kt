package com.loopers.infrastructure.kafka

import org.apache.kafka.clients.consumer.ConsumerRecord

fun ConsumerRecord<String, String>.headerValue(key: String): String? {
    return headers().lastHeader(key)?.value()?.let { String(it, Charsets.UTF_8) }
}

fun ConsumerRecord<String, String>.requireHeaderValue(key: String): String {
    return headerValue(key)
        ?: throw IllegalArgumentException("필수 헤더 누락 [key=$key, topic=${topic()}, offset=${offset()}]")
}
