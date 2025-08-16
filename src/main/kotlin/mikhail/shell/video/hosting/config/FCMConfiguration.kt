package mikhail.shell.video.hosting.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import java.io.File
import java.io.FileInputStream

@Configuration
class FCMConfiguration {
    @Value("\${FCM_CONFIG}")
    private lateinit var FCM_CONFIG: String

    @Bean
    fun firebaseApplication(): FirebaseApp {
        val inputStream = ClassPathResource(FCM_CONFIG).inputStream
        val firebaseOptions = FirebaseOptions.builder()
            .setCredentials(
                GoogleCredentials.fromStream(inputStream)
            ).build()
        return FirebaseApp.initializeApp(firebaseOptions, Companion.FIREBASE_APP)
    }
    @Bean
    fun fcm(): FirebaseMessaging {
        val app = firebaseApplication()
        return FirebaseMessaging.getInstance(app)
    }

    private companion object {
        val FIREBASE_APP = "hosting"
    }
}