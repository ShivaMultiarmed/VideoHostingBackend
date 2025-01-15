package mikhail.shell.video.hosting.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EnableJpaRepositories(
    basePackages = ["mikhail.shell.video.hosting.repository"]
)
class DatabaseConfiguration