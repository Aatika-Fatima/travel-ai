package com.travel.common.util

import tools.jackson.core.JsonGenerator
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.deser.std.StdDeserializer
import tools.jackson.databind.module.SimpleModule
import tools.jackson.databind.ser.std.StdSerializer
import kotlin.uuid.Uuid

// Jackson has no built-in (de)serializer for kotlin.uuid.Uuid -- without
// this it falls back to reflecting mostSignificantBits/leastSignificantBits
// as a plain object, which breaks both REST responses (an unusable id shape)
// and any inter-service Kafka payload that embeds a Uuid field directly,
// since the receiving side's mirror DTO expects the standard string form.
class KotlinUuidModule : SimpleModule("KotlinUuidModule") {
    init {
        addSerializer(Uuid::class.java, UuidSerializer)
        addDeserializer(Uuid::class.java, UuidDeserializer)
    }
}

private object UuidSerializer : StdSerializer<Uuid>(Uuid::class.java) {
    override fun serialize(value: Uuid, gen: JsonGenerator, ctxt: SerializationContext) {
        gen.writeString(value.toString())
    }
}

private object UuidDeserializer : StdDeserializer<Uuid>(Uuid::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Uuid = Uuid.parse(p.string)
}
