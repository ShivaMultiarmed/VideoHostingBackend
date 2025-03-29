package mikhail.shell.video.hosting.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.stereotype.Component
import java.util.*

@Component
class JwtTokenUtil {

    fun generateToken(username: String): String {
        val now = Date()
        //val expiration = Date(now.toInstant().toEpochMilli() + tokenExpirationDuration)
        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(now)
            //.setExpiration(expiration) // TODO Handle new token after it is expired
            .signWith(SignatureAlgorithm.HS256, secret)
            .compact()
    }

    fun validateToken(userId: String, token: String): Boolean {
        val extractedUserId = Jwts.parser().setSigningKey(secret).parseClaimsJwt(token).body.subject
        val extractedDate = Jwts.parser().setSigningKey(secret).parseClaimsJwt(token).body.expiration
        return userId == extractedUserId && Date() < extractedDate
    }

    fun extractUserId(token: String): Long {
        return Jwts.parser().setSigningKey(secret).parseClaimsJwt(token).body.subject.toLong()
    }

    companion object {
        private const val tokenExpirationDuration = 1000L * 60 * 60 * 24 * 30
        private const val secret = "PJdHAxkeLehwQhEi3fTkCG2r8yrqtT8g"
    }
}