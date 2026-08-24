package com.travel.app

import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.format.FormatterRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import kotlin.uuid.Uuid

// Spring's ConversionService has a built-in String -> java.util.UUID
// converter, but no equivalent for kotlin.uuid.Uuid. Every module wired
// into this app (BookingController, PaymentController, ...) takes
// @PathVariable ...: Uuid, so without this every one of those endpoints
// 500s on a perfectly valid UUID path segment.
@Configuration
class WebMvcConfig : WebMvcConfigurer {
    override fun addFormatters(registry: FormatterRegistry) {
        // A lambda erases its generic signature at runtime, which is exactly
        // what GenericConversionService reflects on to find <S, T> -- so this
        // has to be a real class implementing Converter, not `Converter { }`.
        registry.addConverter(StringToUuidConverter)
    }
}

private object StringToUuidConverter : Converter<String, Uuid> {
    override fun convert(source: String): Uuid = Uuid.parse(source)
}
