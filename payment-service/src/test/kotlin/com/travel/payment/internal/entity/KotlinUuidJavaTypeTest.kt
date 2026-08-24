package com.travel.payment.internal.entity

import org.hibernate.type.descriptor.WrapperOptions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import java.util.UUID as JavaUUID

class KotlinUuidJavaTypeTest {
    private val type = KotlinUuidJavaType()
    private val options = mock<WrapperOptions>()
    private val uuid = Uuid.parse("123e4567-e89b-12d3-a456-426614174000")

    @Test
    fun `fromString parses a Uuid`() {
        assertEquals(uuid, type.fromString(uuid.toString()))
    }

    @Test
    fun `unwrap returns null for a null value`() {
        assertNull(type.unwrap(null, JavaUUID::class.java, options))
    }

    @Test
    fun `unwrap returns the value unchanged when the target type already matches`() {
        assertEquals(uuid, type.unwrap(uuid, Uuid::class.java, options))
    }

    @Test
    fun `unwrap converts to java-util-UUID`() {
        assertEquals(uuid.toJavaUuid(), type.unwrap(uuid, JavaUUID::class.java, options))
    }

    @Test
    fun `unwrap converts to String`() {
        assertEquals(uuid.toString(), type.unwrap(uuid, String::class.java, options))
    }

    @Test
    fun `unwrap throws for an unsupported target type`() {
        assertFails { type.unwrap(uuid, Int::class.java, options) }
    }

    @Test
    fun `wrap returns null for a null value`() {
        assertNull(type.wrap<Uuid>(null, options))
    }

    @Test
    fun `wrap returns a Uuid value unchanged`() {
        assertEquals(uuid, type.wrap(uuid, options))
    }

    @Test
    fun `wrap converts from java-util-UUID`() {
        assertEquals(uuid, type.wrap(uuid.toJavaUuid(), options))
    }

    @Test
    fun `wrap parses a String`() {
        assertEquals(uuid, type.wrap(uuid.toString(), options))
    }

    @Test
    fun `wrap throws for an unsupported source type`() {
        assertFails { type.wrap(42, options) }
    }
}
