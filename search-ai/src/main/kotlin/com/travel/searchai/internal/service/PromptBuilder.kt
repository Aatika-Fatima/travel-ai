package com.travel.searchai.internal.service

import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.chat.prompt.PromptTemplate
import org.springframework.stereotype.Component

@Component
class PromptBuilder(val promptLoader: PromptLoader){
    fun renderSystemPrompt(definition: PromptDefinition, context:PromptContext): String{
        return render(promptLoader.load(definition.systemPath),
            context.getContext())
    }

    fun renderUserPrompt(definition: PromptDefinition, context:PromptContext):String{
        return render(promptLoader.load(definition.userPath),
            context.getContext())
    }

    fun renderBusinessPrompt(definition: PromptDefinition, context:PromptContext): String{
        return render(promptLoader.load(definition.businessPath),
            context.getContext())
    }

    fun assemblePrompt(systemAndBusinessPrompt:String,definition: PromptDefinition, context:PromptContext):Prompt{

        val userPrompt = renderUserPrompt(definition, context)
        return Prompt(listOf(
            SystemMessage(systemAndBusinessPrompt),
            UserMessage(userPrompt)))
    }

      fun renderSystemAndBusiness(
        promptDefinition: PromptDefinition,
        promptContext: PromptContext
    ): String {
        val systemPrompt = renderSystemPrompt(promptDefinition, promptContext)
        val business = renderBusinessPrompt(promptDefinition, promptContext)
        return listOf(systemPrompt, business).joinToString(separator = "\n\n")
    }

    private fun render(template: String, variables: Map<String, Any>): String = PromptTemplate(template).render(variables)

}