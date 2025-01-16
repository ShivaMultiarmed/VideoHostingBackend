package mikhail.shell.video.hosting.config

import mikhail.shell.video.hosting.elastic.repository.coverters.LocalDateTimeToSecondsConverter
import mikhail.shell.video.hosting.elastic.repository.coverters.SecondsToLocalDateTimeConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories

@Configuration
@EnableElasticsearchRepositories(
    basePackages = ["mikhail.shell.video.hosting.elastic.repository"]
)
class ElasticConfiguration
//    : AbstractElasticsearchConfiguration()
    {
//    @Bean(name = [ "hostingSearchClient" ])
//    override fun elasticsearchClient(): RestHighLevelClient {
//        val clientConfiguration = ClientConfiguration.builder()
//            .connectedTo("elastic_search:9200")
//            .build();
//
//        return RestClients.create(clientConfiguration)
//            .rest()
//    }

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