package mikhail.shell.video.hosting.elastic.repository.converters


import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset

@WritingConverter @Component
class LocalDateTimeToSecondsConverter: Converter<LocalDateTime, Long> {
    override fun convert(source: LocalDateTime): Long? {
        return source.toEpochSecond(ZoneOffset.UTC)
    }
}
@ReadingConverter @Component
class SecondsToLocalDateTimeConverter: Converter<Long, LocalDateTime> {
    override fun convert(source: Long): LocalDateTime? {
        return LocalDateTime.ofEpochSecond(source, 0, ZoneOffset.UTC)
    }
}