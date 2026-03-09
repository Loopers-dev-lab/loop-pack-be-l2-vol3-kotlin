package com.loopers

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import java.util.TimeZone

@ConfigurationPropertiesScan
@SpringBootApplication
@EnableCaching
class CommerceApiApplication

fun main(args: Array<String>) {
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
    runApplication<CommerceApiApplication>(*args)
}
