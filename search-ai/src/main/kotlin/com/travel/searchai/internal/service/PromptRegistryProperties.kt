package com.travel.searchai.internal.service

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "prompts")
class PromptRegistryProperties (
    val activeVersions: Map<String,String> = mapOf("flight-search" to "flight-search-v1",)
)

