package com.travel.payment.internal.entity

import org.hibernate.type.descriptor.WrapperOptions
import org.hibernate.type.descriptor.java.AbstractClassJavaType
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid
import java.util.UUID as JavaUUID

// Ported from booking-service's KotlinUuidJavaType -- see that file's
// comment for the full story. Hibernate has no built-in JavaType for
// kotlin.uuid.Uuid, and a plain JPA AttributeConverter can't stand in for
// it on an @Id attribute -- Hibernate 7 rejects AttributeConverter there
// outright (KotlinUuidAttributeConverter covers every non-Id Uuid column
// instead). This registers kotlin.uuid.Uuid as a first-class Hibernate
// type so it can bind directly to Postgres's native uuid column -- see
// Payment.id for the @JavaType/@JdbcTypeCode usage site.
class KotlinUuidJavaType : AbstractClassJavaType<Uuid>(Uuid::class.java, ImmutableMutabilityPlan.instance()) {

    override fun fromString(string: CharSequence): Uuid = Uuid.parse(string.toString())

    override fun <X> unwrap(value: Uuid?, type: Class<X>, options: WrapperOptions): X? {
        if (value == null) return null
        return when {
            type.isInstance(value) -> type.cast(value)
            type == JavaUUID::class.java -> type.cast(value.toJavaUuid())
            type == String::class.java -> type.cast(value.toString())
            else -> throw unknownUnwrap(type)
        }
    }

    override fun <X> wrap(value: X?, options: WrapperOptions): Uuid? {
        if (value == null) return null
        return when (value) {
            is Uuid -> value
            is JavaUUID -> value.toKotlinUuid()
            is String -> Uuid.parse(value)
            else -> throw unknownWrap(value.javaClass)
        }
    }
}
