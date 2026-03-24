package mikhail.shell.video.hosting.config

import org.springframework.boot.convert.ApplicationConversionService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import tools.jackson.databind.JacksonModule
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueSerializer
import tools.jackson.databind.module.SimpleModule

@Configuration
class WebConfiguration : WebMvcConfigurer {
    override fun addFormatters(registry: FormatterRegistry) {
        ApplicationConversionService.configure(registry)
    }

    @Bean
    fun jsonModule(): JacksonModule {
        val module = SimpleModule("JsonModule")
        val serializer = object : ValueSerializer<Enum<*>>() {
            override fun serialize(
                value: Enum<*>?,
                gen: tools.jackson.core.JsonGenerator?,
                ctxt: SerializationContext?
            ) {
                gen?.writeString(value?.name?.lowercase())
            }
        }
        module.addSerializer(Enum::class.java, serializer)
        return module
    }
}