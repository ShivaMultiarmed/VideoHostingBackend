package mikhail.shell.video.hosting.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*

@Component
class JwtTokenUtil(
    @Value("\${CRYPTO_KEY}") private val CRYPTO_KEY: String
) {
    private val parser = Jwts
        .parser()
        .setSigningKey(CRYPTO_KEY)
        .setAllowedClockSkewSeconds(30)

    fun generateToken(
        username: String,
        expirationPeriod: Long = AUTH_TOKEN_EXPIRATION_DURATION
    ): String {
        val now = Date()
        val expiration = Date(now.toInstant().toEpochMilli() + expirationPeriod)
        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(now)
            .setExpiration(expiration)
            .signWith(SignatureAlgorithm.HS256, CRYPTO_KEY)
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
        val expireDate = claims.expiration?: return false
        val now = Date()
        return issuedDate < now && now < expireDate
    }

    fun extractUserId(token: String): Long? {
        return parseClaims(token)?.subject?.toLong()
    }

    companion object {
        const val AUTH_TOKEN_EXPIRATION_DURATION = 1000L * 60 * 60 * 24 * 30
    }
}