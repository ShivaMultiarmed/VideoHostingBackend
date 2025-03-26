package mikhail.shell.video.hosting.repository.converters

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant

@Converter(autoApply = true)
class InstantConverter: AttributeConverter<kotlinx.datetime.Instant, java.time.Instant> {
    override fun convertToDatabaseColumn(kotlinInstant: kotlinx.datetime.Instant?): java.time.Instant? {
        return kotlinInstant?.toJavaInstant()
    }

    override fun convertToEntityAttribute(javaInstant: java.time.Instant?): kotlinx.datetime.Instant? {
        return javaInstant?.toKotlinInstant()
    }
}