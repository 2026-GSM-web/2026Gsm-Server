package org.example.schoolweb

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class SchoolwebApplication

fun main(args: Array<String>) {
    runApplication<SchoolwebApplication>(*args)
}
