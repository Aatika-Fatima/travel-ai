package com.travel.duffel.api.dto.response

import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.deser.std.StdDeserializer

class DuffelConditionDetailDeserializer :
    StdDeserializer<DuffelConditionDetail>(DuffelConditionDetail::class.java) {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): DuffelConditionDetail {
        val node = ctxt.readTree(p)

        if (node.isBoolean) {
            return DuffelConditionDetail(allowed = node.booleanValue())
        }

        return DuffelConditionDetail(
            allowed = node.path("allowed").booleanValue(),
            penaltyAmount = node.path("penalty_amount").asString(null),
            penaltyCurrency = node.path("penalty_currency").asString(null),
        )
    }
}