package mikhail.shell.video.hosting.config

import org.hibernate.boot.model.FunctionContributions
import org.hibernate.dialect.MySQLDialect
import org.hibernate.dialect.function.StandardSQLFunction
import org.hibernate.type.StandardBasicTypes
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EnableJpaRepositories(
    basePackages = ["mikhail.shell.video.hosting.repository"]
)
class DatabaseConfiguration