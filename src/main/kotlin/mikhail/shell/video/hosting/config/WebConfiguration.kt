package mikhail.shell.video.hosting.config

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfiguration : WebMvcConfigurer {
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/resources/**")
            .addResourceLocations("classpath:/resources/")
    }
    @Bean
    fun lowerSnakeEnumModule(): Module {
        val module = SimpleModule()
        module.addSerializer(
            Enum::class.java, object : JsonSerializer<Enum<*>?>() {
                override fun serialize(
                    value: Enum<*>?,
                    gen: JsonGenerator,
                    serializers: SerializerProvider,
                ) {
                    val lowerSnake = value?.name?.lowercase()?: return gen.writeNull()
                    gen.writeString(lowerSnake)
                }
            }
        )
        module.addDeserializer(
            Enum::class.java, object : JsonDeserializer<Enum<*>?>() {
                override fun deserialize(
                    p: JsonParser?, ctxt: DeserializationContext?
                ): Enum<*>? {
                    val raw = p?.valueAsString ?: return null
                    val enumClass = ctxt?.contextualType?.rawClass as? Class<out Enum<*>> ?: return null
                    return java.lang.Enum.valueOf(enumClass, raw.uppercase())
                }
            }
        )
        return module
    }
}