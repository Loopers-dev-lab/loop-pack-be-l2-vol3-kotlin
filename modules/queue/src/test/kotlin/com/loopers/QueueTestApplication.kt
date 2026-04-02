package com.loopers

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class QueueTestApplication

fun main(args: Array<String>) {
    runApplication<QueueTestApplication>(*args)
}
