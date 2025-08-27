package mikhail.shell.video.hosting.service

import jakarta.transaction.Transactional
import kotlinx.datetime.Clock
import kotlinx.datetime.toKotlinInstant
import mikhail.shell.video.hosting.domain.AuthModel
import mikhail.shell.video.hosting.domain.User
import mikhail.shell.video.hosting.domain.ValidationRules
import mikhail.shell.video.hosting.errors.*
import mikhail.shell.video.hosting.errors.SignUpError.*
import mikhail.shell.video.hosting.repository.*
import mikhail.shell.video.hosting.entities.InvalidTokenEntity
import mikhail.shell.video.hosting.entities.VerificationCodePurpose
import mikhail.shell.video.hosting.entities.VerificationEntity
import mikhail.shell.video.hosting.security.CryptoUtils
import mikhail.shell.video.hosting.security.JwtTokenUtil
import mikhail.shell.video.hosting.security.JwtTokenUtil.Companion.SHORT_LIVED_TOKEN_EXPIRATION_DURATION
import org.springframework.mail.MailSender
import org.springframework.mail.SimpleMailMessage
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@Service
class AuthServiceWithDB(
    private val jwtTokenUtil: JwtTokenUtil,
    private val authDetailRepository: AuthDetailRepository,
    private val passwordRepository: PasswordRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val mailSender: MailSender,
    private val cryptoUtils: CryptoUtils,
    private val verificationRepository: VerificationRepository,
    private val invalidTokenRepository: InvalidTokenRepository
) : AuthService {

    override fun signInWithPassword(userName: String, password: String): AuthModel {
        val method = if (userName.matches(ValidationRules.EMAIL_REGEX)) AuthenticationMethod.EMAIL else AuthenticationMethod.TEL
        val authDetailEntity = authDetailRepository.findByUserNameAndId_Method(userName, method).orElseThrow()
        val userId = authDetailEntity.id.userId
        val expectedPassword = passwordRepository.findById(userId).orElseThrow().password
        if (!passwordEncoder.matches(password, expectedPassword)) {
            throw IllegalArgumentException()
        } else {
            val token = jwtTokenUtil.generateToken(userId.toString())
            return AuthModel(token, userId)
        }
    }

    override fun signOut(token: String) {
        invalidTokenRepository.save(InvalidTokenEntity(token))
    }

    @Transactional
    override fun requestSignUpWithPassword(userName: String) {
        if (authDetailRepository.existsByUserNameAndId_Method(userName, AuthenticationMethod.EMAIL)) {
            throw UniquenessViolationException()
        }
        val code = cryptoUtils.generateString(4)
        SimpleMailMessage().apply {
            setTo(userName)
            from = "trendy@no-reply.com"
            subject = "Signing up"
            text = "Code to proceed signing up: $code"
            mailSender.send(this)
        }
        verificationRepository.save(
            VerificationEntity(
                username = userName,
                purpose = VerificationCodePurpose.SIGNUP,
                issuedAt = Instant.now(),
                code = passwordEncoder.encode(code)
            )
        )
    }

    override fun verifySignUpWithPassword(userName: String, code: String): String {
        if (authDetailRepository.existsByUserNameAndId_Method(userName, AuthenticationMethod.EMAIL)) {
            throw UniquenessViolationException()
        }
        val verificationEntity = verificationRepository
            .findByUserNameAndPurpose(
                userName = userName,
                purpose = VerificationCodePurpose.SIGNUP
            ).orElseThrow {
                IllegalArgumentException()
            }
        verificationRepository.delete(verificationEntity)
        return jwtTokenUtil.generateToken(userName, SHORT_LIVED_TOKEN_EXPIRATION_DURATION)
    }

    override fun confirmSignUpWithPassword(
        token: String,
        password: String,
        user: User
    ): AuthModel {
        val userName = jwtTokenUtil.extractSubject(token)
        if (authDetailRepository.existsByUserNameAndId_Method(userName, AuthenticationMethod.EMAIL)) {
            throw UniquenessViolationException()
        }
        if (userRepository.existsByNick(user.nick)) {
            throw UniquenessViolationException()
        }
        val userId = userRepository.save(user.toEntity()).userId!!
        authDetailRepository.save(AuthDetailEntity(AuthDetailEntityId(userId, AuthenticationMethod.EMAIL), userName!!))
        passwordRepository.save(PasswordEntity(userId, passwordEncoder.encode(password)))
        val authToken = jwtTokenUtil.generateToken(userId.toString())
        return AuthModel(authToken, userId)
    }

    override fun requestPasswordReset(userName: String) {
        val method = when {
            userName.matches(ValidationRules.EMAIL_REGEX) -> AuthenticationMethod.EMAIL
            else -> throw ValidationException(ResetError.USERNAME_MALFORMED)
        }
        if (!authDetailRepository.existsByUserNameAndId_Method(userName, method)) {
            throw ValidationException(ResetError.USERNAME_NOT_FOUND)
        }
        val resetCode = cryptoUtils.generateString(4)
        SimpleMailMessage().apply {
            setTo(userName)
            from = "trendy@no-reply.com"
            subject = "Password recovery"
            text = "Your password recovery password: $resetCode"
            mailSender.send(this)
        }
        verificationRepository.save(
            VerificationEntity(
                username = userName,
                issuedAt = Instant.now(),
                purpose = VerificationCodePurpose.RESET,
                code = passwordEncoder.encode(resetCode)
            )
        )
    }

    override fun verifyPasswordReset(userName: String, code: String): String {
        val verificationEntity = verificationRepository.findByUserNameAndPurpose(
            userName = userName,
            purpose = VerificationCodePurpose.RESET
        ).orElseThrow { ValidationException(ResetError.CODE_NOT_VALID) }
        if (!passwordEncoder.matches(code, verificationEntity.code)) {
            throw ValidationException(ResetError.CODE_NOT_CORRECT)
        } else if (verificationEntity.issuedAt.toKotlinInstant() + SHORT_LIVED_TOKEN_EXPIRATION_DURATION < Clock.System.now()) {
            throw ValidationException(ResetError.CODE_NOT_VALID)
        } else {
            verificationRepository.delete(verificationEntity)
            return jwtTokenUtil.generateToken(userName, SHORT_LIVED_TOKEN_EXPIRATION_DURATION)
        }
    }

    override fun resetPassword(token: String, password: String) {
        if (!jwtTokenUtil.validateToken(token) || invalidTokenRepository.existsById(token)) {
            throw ValidationException(ResetError.TOKEN_NOT_VALID)
        }
        if (!password.matches(ValidationRules.PASSWORD_REGEX)) {
            throw ValidationException(ResetError.PASSWORD_NOT_VALID)
        }
        val userName = jwtTokenUtil.extractSubject(token)!!
        val userId = authDetailRepository
            .findByUserName(userName)
            .orElseThrow { ValidationException(ResetError.USERNAME_NOT_FOUND) }
            .id.userId
        passwordRepository
            .findById(userId)
            .get()
            .copy(
                password = passwordEncoder.encode(password)
            ).let {
                passwordRepository.save(it)
            }
        invalidTokenRepository.save(InvalidTokenEntity(token))
    }
}