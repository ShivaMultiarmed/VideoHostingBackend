package mikhail.shell.video.hosting

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(
	exclude = [
		org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration::class
	]
)
class Application

fun main(args: Array<String>) {
	runApplication<Application>(*args)
}

