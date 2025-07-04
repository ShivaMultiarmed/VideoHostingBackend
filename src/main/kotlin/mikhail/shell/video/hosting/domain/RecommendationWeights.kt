package mikhail.shell.video.hosting.domain

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "hosting.application.ranking.weights")
class RecommendationWeights {
    var dateTime = 0.0
    var subscribers = 0.0
    var views = 0.0
    var likes = 0.0
    var dislikes = 0.0
}