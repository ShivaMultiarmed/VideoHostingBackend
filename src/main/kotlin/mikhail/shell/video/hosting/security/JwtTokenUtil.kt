package mikhail.shell.video.hosting.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

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
        expirationDuration: ExpirationDuration = ExpirationDuration.LONG
    ): String {
        val now = Clock.System.now()
        val expiration = now + expirationDuration.duration
        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(now.toDate())
            .setExpiration(expiration.toDate())
            .signWith(SignatureAlgorithm.HS256, CRYPTO_KEY)
            .compact()
    }

    private fun Instant.toDate(): Date = Date(toJavaInstant().toEpochMilli())

    fun parseClaims(token: String): Claims? {
        return try {
            parser.parseClaimsJws(token).body
        } catch (e: Exception) {
            null
        }
    }

    fun validateToken(token: String): Boolean {
        val claims = parseClaims(token)?: return false
        claims.subject?: return false
        val issuedDate = claims.issuedAt?: return false
        val expireDate = claims.expiration?: return false
        val now = Date()
        return issuedDate < now && now < expireDate
    }

    fun extractUserId(token: String): Long? {
        return extractSubject(token)?.toLongOrNull()
    }

    fun extractSubject(token: String): String? {
        return parseClaims(token)?.subject
    }
}

enum class ExpirationDuration(val duration: Duration) {
    LONG(30.days),
    SHORT(10.minutes)
}