package com.travel.common.entity

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

// Same problem and same reasoning as KotlinUuidAttributeConverter, for
// kotlin.time.Instant -- lives in common so only one auto-apply converter
// for this attribute type ever exists across every module merged into
// app's shared persistence unit.
@Converter(autoApply = true)
class KotlinInstantAttributeConverter : AttributeConverter<Instant, java.time.Instant> {
    override fun convertToDatabaseColumn(attribute: Instant?): java.time.Instant? = attribute?.toJavaInstant()

    override fun convertToEntityAttribute(dbData: java.time.Instant?): Instant? = dbData?.toKotlinInstant()
}
