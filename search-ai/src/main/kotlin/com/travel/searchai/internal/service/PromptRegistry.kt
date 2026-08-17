package com.travel.searchai.internal.service

interface PromptRegistry {
    fun getPrompt(name:String): PromptDefinition
}