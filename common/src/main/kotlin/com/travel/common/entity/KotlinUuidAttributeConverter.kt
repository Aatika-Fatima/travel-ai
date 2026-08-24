package com.travel.common.entity

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.util.UUID
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

// Hibernate has no built-in mapping for kotlin.uuid.Uuid (only java.util.UUID)
// -- without this, every Uuid-typed non-Id column falls back to a generic
// binary (bytea) representation instead of Postgres's native uuid type.
// Lives in common, not any one module, because autoApply = true converters
// register per attribute *type* for the whole persistence unit -- once more
// than one module is merged into app's shared context, two independently
// declared auto-apply converters for the same (Uuid, UUID) pair collide
// ("multiple auto-apply converters found"), regardless of their class names
// or packages. One shared converter is the only valid way to have more than
// one Uuid-using module in the same app process.
// Uuid @Id attributes need KotlinUuidJavaType instead -- Hibernate 7 rejects
// an AttributeConverter on an @Id attribute outright.
@Converter(autoApply = true)
class KotlinUuidAttributeConverter : AttributeConverter<Uuid, UUID> {
    override fun convertToDatabaseColumn(attribute: Uuid?): UUID? = attribute?.toJavaUuid()

    override fun convertToEntityAttribute(dbData: UUID?): Uuid? = dbData?.toKotlinUuid()
}
