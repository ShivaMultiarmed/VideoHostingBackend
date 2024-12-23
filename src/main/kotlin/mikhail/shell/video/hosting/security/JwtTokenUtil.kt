package mikhail.shell.video.hosting.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.stereotype.Component
import java.util.*

@Component
class JwtTokenUtil {

    fun generateToken(username: String): String {
        val now = Date()
        val expiration = Date(now.toInstant().toEpochMilli() + tokenExpirationDuration)
                return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(now)
            .setExpiration(expiration)
            .signWith(SignatureAlgorithm.HS256, secret)
            .compact()
    }

    fun validateToken(username: String, token: String): Boolean {
        val extractedName = Jwts.parser().setSigningKey(secret).parseClaimsJwt(token).body.subject
        val extractedDate = Jwts.parser().setSigningKey(secret).parseClaimsJwt(token).body.expiration
        return username == extractedName && Date() < extractedDate
    }

    companion object {
        private const val tokenExpirationDuration = 60 * 60 * 24 * 10 * 1000
        private const val secret = "PJdHAxkeLehwQhEi3fTkCG2r8yrqtT8g"
    }
}