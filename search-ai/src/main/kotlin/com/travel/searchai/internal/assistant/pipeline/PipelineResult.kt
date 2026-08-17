package com.travel.searchai.internal.assistant.pipeline

import com.travel.searchai.internal.assistant.AssistantMessageResponse

// A pipeline stage either produces a value for the next stage to consume, or halts the chain
// with a terminal response (e.g. "I couldn't find that airport").
sealed interface PipelineResult<out T> {
    data class Proceed<T>(val value: T) : PipelineResult<T>

    data class Respond(val response: AssistantMessageResponse) : PipelineResult<Nothing>
}

inline fun <T, R> PipelineResult<T>.andThen(next: (T) -> PipelineResult<R>): PipelineResult<R> =
    when (this) {
        is PipelineResult.Proceed -> next(value)
        is PipelineResult.Respond -> this
    }

inline fun <T, R> PipelineResult<T>.map(transform: (T) -> R): PipelineResult<R> = andThen { PipelineResult.Proceed(transform(it)) }