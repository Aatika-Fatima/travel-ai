package com.travel.searchai.internal.service

class ConventionPromptRegistry(private val properties: PromptRegistryProperties): PromptRegistry{
    override fun getPrompt(name: String): PromptDefinition {
       val version = properties.activeVersions[name]?:error("no active version for $name")
        return PromptDefinition(
            name = name,
            version = version,
            systemPath = "classpath:prompt/$name/system/$version.st",
            businessPath = "classpath:prompt/$name/business/$version.st",
            userPath = "classpath:prompt/$name/user/$name-user.st",
        )
     }

}