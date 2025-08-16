package mikhail.shell.video.hosting.config

import mikhail.shell.video.hosting.elastic.repository.converters.LocalDateTimeToSecondsConverter
import mikhail.shell.video.hosting.elastic.repository.converters.SecondsToLocalDateTimeConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories

@Configuration
@EnableElasticsearchRepositories(
    basePackages = ["mikhail.shell.video.hosting.elastic.repository"]
)
class ElasticConfiguration {
    @Bean
    fun elasticsearchCustomConversions(): ElasticsearchCustomConversions {
        return ElasticsearchCustomConversions(
            listOf(
                LocalDateTimeToSecondsConverter(),
                SecondsToLocalDateTimeConverter()
            )
        )
    }
}
