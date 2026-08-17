package com.travel.searchai.internal.service

import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Component

@Component
class PromptLoader(private val resourceLoader: ResourceLoader) {

    fun load(path: String): String {
        return resourceLoader.getResource(path).inputStream.bufferedReader().readText()
    }
}