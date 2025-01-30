package mikhail.shell.video.hosting.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.FileInputStream

@Configuration
class FCMConfiguration {
    @Value("\${hosting.application.path}")
    private lateinit var PROJECT_ROOT: String
    @Value("\${FCM_CONFIG}")
    private lateinit var FCM_CONFIG: String
    private val FIREBASE_APP = "hosting"
    @Bean
    fun firebaseApplication(): FirebaseApp {
        val inputStream = FileInputStream("$PROJECT_ROOT/$FCM_CONFIG")
        val firebaseOptions = FirebaseOptions.builder()
            .setCredentials(
                GoogleCredentials.fromStream(inputStream)
            ).build()
        return FirebaseApp.initializeApp(firebaseOptions, )
    }
    @Bean
    fun fcm(): FirebaseMessaging {
        val app = FirebaseApp.getInstance(FIREBASE_APP)
        return FirebaseMessaging.getInstance(app)
    }
}