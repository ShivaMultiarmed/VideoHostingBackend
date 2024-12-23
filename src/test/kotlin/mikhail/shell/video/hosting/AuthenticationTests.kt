package mikhail.shell.video.hosting

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

class AuthenticationTests {

    @Test
    fun createPassword() {
        val password = "qwerty"
        println(BCryptPasswordEncoder().encode(password))
    }
}