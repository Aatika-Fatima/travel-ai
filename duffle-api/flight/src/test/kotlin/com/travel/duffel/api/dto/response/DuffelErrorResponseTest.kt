package com.travel.duffel.api.dto.response

import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DuffelErrorResponseTest {
    private val mapper = JsonMapper.builder().build()

    @Test
    fun `parses an airline error where source is a bare string`() {
        val body =
            """
            {"errors":[{"documentation_url":"https://duffel.com/docs",
              "source":"travelport","title":"Requested offer is no longer available",
              "type":"airline_error","code":"offer_no_longer_available",
              "message":"Please select another offer."}],
             "meta":{"request_id":"abc","status":422}}
            """.trimIndent()

        val parsed = mapper.readValue(body, DuffelErrorResponse::class.java)

        assertEquals(1, parsed.errors.size)
        assertEquals("offer_no_longer_available", parsed.errors[0].code)
        assertNull(parsed.errors[0].source) // a string source doesn't narrow to the object form
    }

    @Test
    fun `parses a validation error where source is an object`() {
        val body =
            """
            {"errors":[{"type":"validation_error","code":"invalid",
              "title":"Invalid","message":"bad phone",
              "source":{"field":"phone_number","pointer":"/data/passengers/0"}}]}
            """.trimIndent()

        val parsed = mapper.readValue(body, DuffelErrorResponse::class.java)

        assertEquals("phone_number", parsed.errors[0].source?.field)
    }
}
