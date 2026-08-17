package com.travel.searchai.internal.service

import org.springframework.ai.chat.prompt.Prompt
import org.springframework.stereotype.Component

@Component
class PromptOrchestrator(
    private val promptRegistry: PromptRegistry,
    private val promptBuilder: PromptBuilder,
    private val promptCache: PromptCache,
    private val contextBuilder: ContextBuilder,
    private val guardRails: PromptGuardRails
){
    fun build(promptName:String,userMessage:String,sessionId:String?=null): Prompt {
        val promptDefinition = promptRegistry.getPrompt(promptName)
        val promptContext = contextBuilder.build(sessionId, userMessage)
        val cacheKey="$promptName:${promptDefinition.version}"
        val systemAndBusiness =
            promptCache.get(cacheKey)?:
                promptBuilder.renderSystemAndBusiness(promptDefinition, promptContext)
            .also { promptCache.put(cacheKey, it) }
        val prompt = promptBuilder.assemblePrompt(systemAndBusiness,promptDefinition,promptContext)
        guardRails.check(userMessage)
        return prompt;
      }


}