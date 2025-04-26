package mikhail.shell.video.hosting.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.stereotype.Component
import java.util.*

@Component
class JwtTokenUtil {

    private val parser = Jwts
        .parser()
        .setSigningKey(secret)
        .setAllowedClockSkewSeconds(30)

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

    fun parseClaims(token: String): Claims? {
        return try {
            parser.parseClaimsJws(token).body
        } catch (e: Exception) {
            null
        }
    }

    fun validateToken(token: String): Boolean {
        val claims = parseClaims(token)?: return false
        claims.subject?.toLongOrNull()?: return false
        val issuedDate = claims.issuedAt?: return false
        // val expireDate = claims.expiration?: return false
        val now = Date()
        return issuedDate < now // && Date() < expireDate
    }

    fun extractUserId(token: String): Long? {
        return parseClaims(token)?.subject?.toLong()
    }

    companion object {
        private const val tokenExpirationDuration = 1000L * 60 * 60 * 24 * 30
        private const val secret = "PJdHAxkeLehwQhEi3fTkCG2r8yrqtT8g"
    }
}