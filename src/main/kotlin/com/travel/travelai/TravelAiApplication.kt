package com.travel.travelai

import org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration
import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.io.File

@SpringBootApplication(exclude = [ChatClientAutoConfiguration::class, OpenAiEmbeddingAutoConfiguration::class])
class TravelAiApplication

// Spring Boot does not load .env files itself; IDE run configurations often
// inject them, but plain `mvnw`/CLI runs don't. Load it into system
// properties so ${VAR} placeholders in application.yaml resolve either way.
private fun loadDotEnv() {
    val envFile = File(".env")
    if (!envFile.exists()) return
    envFile.forEachLine { line ->
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEachLine
        val idx = trimmed.indexOf('=')
        if (idx <= 0) return@forEachLine
        val key = trimmed.substring(0, idx).trim()
        val value = trimmed.substring(idx + 1).trim()
        if (System.getProperty(key) == null && System.getenv(key) == null) {
            System.setProperty(key, value)
        }
    }
}

fun main(args: Array<String>) {
    loadDotEnv()
    runApplication<TravelAiApplication>(*args)
}
