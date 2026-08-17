package com.travel.searchai.internal.assistant

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/assistant")
class AssistantController(
    private
    val assistantService: FlightSearchAssistantService,
) {
    @PostMapping("/message")
    fun message(
        @RequestBody request: AssistantMessageRequest,
    ): AssistantMessageResponse = assistantService.handle(request.message)
}
