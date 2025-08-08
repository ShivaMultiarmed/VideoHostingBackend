package mikhail.shell.video.hosting.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.SecureRandom

@Component
class CryptoUtils(
    @Value("\${CRYPTO_KEY}") private val CRYPTO_KEY: String
) {
    private val random = SecureRandom()
    fun generateChar(): Char {
        val generateNumber = random.nextFloat() > (26 * 2).toFloat() / (10 + 26 * 2)
        return if (generateNumber) {
            '0' + random.nextInt(10)
        } else {
            val smallLetter = random.nextFloat() > 0.5f
            (if (smallLetter) 'a' else 'A') + random.nextInt(26)
        }
    }
    fun generateString(length: Int = 4): String {
        require(length >= 1)
        return buildString {
            repeat(length) {
                append(generateChar())
            }
        }
    }
}