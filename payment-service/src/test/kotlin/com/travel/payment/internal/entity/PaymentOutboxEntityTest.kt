package com.travel.payment.internal.entity

import org.junit.jupiter.api.Test
import kotlin.test.assertNull

class PaymentOutboxEntityTest {
    @Test
    fun `the Hibernate-only no-arg constructor bypasses Kotlin's default values entirely`() {
        // kotlin-jpa generates this constructor for every @Entity so Hibernate
        // can instantiate a row via reflection before populating its fields --
        // application code never calls it. It's a raw allocation, not a real
        // invocation of the Kotlin constructor, so "= PENDING" never runs;
        // Hibernate immediately overwrites every field from the result set.
        val ctor = PaymentOutboxEntity::class.java.getDeclaredConstructor()
        ctor.isAccessible = true
        val blank = ctor.newInstance()

        assertNull(blank.status)
    }
}
