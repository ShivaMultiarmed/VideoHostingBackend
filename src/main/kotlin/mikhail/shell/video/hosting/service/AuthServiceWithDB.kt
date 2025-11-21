package mikhail.shell.video.hosting.service

import jakarta.transaction.Transactional
import kotlinx.datetime.Clock
import kotlinx.datetime.toKotlinInstant
import mikhail.shell.video.hosting.domain.AuthModel
import mikhail.shell.video.hosting.domain.UserCreatingModel
import mikhail.shell.video.hosting.domain.ValidationRules
import mikhail.shell.video.hosting.errors.*
import mikhail.shell.video.hosting.repository.*
import mikhail.shell.video.hosting.entities.InvalidTokenEntity
import mikhail.shell.video.hosting.entities.VerificationCodePurpose
import mikhail.shell.video.hosting.entities.VerificationEntity
import mikhail.shell.video.hosting.security.CryptoUtils
import mikhail.shell.video.hosting.security.ExpirationDuration
import mikhail.shell.video.hosting.security.JwtTokenUtil
import org.springframework.mail.MailSender
import org.springframework.mail.SimpleMailMessage
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant

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
        val method =
            if (userName.matches(ValidationRules.EMAIL_REGEX.toRegex())) AuthenticationMethod.EMAIL else AuthenticationMethod.TEL
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
            throw ValidationException(
                mapOf("userName" to TextError.EXISTS)
            )
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
                userName = userName,
                purpose = VerificationCodePurpose.SIGN_UP,
                issuedAt = Instant.now(),
                code = passwordEncoder.encode(code)
            )
        )
    }

    override fun verifySignUpWithPassword(
        userName: String,
        code: String
    ): String {
        if (authDetailRepository.existsByUserNameAndId_Method(userName, AuthenticationMethod.EMAIL)) {
            ValidationException(
                mapOf("userName" to TextError.EXISTS)
            )
        }
        val verificationEntity = verificationRepository.findByUserNameAndPurpose(userName, VerificationCodePurpose.SIGN_UP).orElseThrow()
        if (!passwordEncoder.matches(code, verificationEntity.code)) {
            throw ValidationException(
                mapOf("code" to TextError.NOT_CORRECT)
            )
        }
        verificationRepository.delete(verificationEntity)
        return jwtTokenUtil.generateToken(userName, ExpirationDuration.SHORT)
    }

    override fun confirmSignUpWithPassword(
        token: String,
        password: String,
        user: UserCreatingModel
    ): AuthModel {
        val userName = jwtTokenUtil.extractSubject(token)!!
        val errors = mutableMapOf<String, Error>()
        if (authDetailRepository.existsByUserNameAndId_Method(userName, AuthenticationMethod.EMAIL)) {
            errors["user"] = TextError.EXISTS
        }
        if (userRepository.existsByNick(user.nick)) {
            errors["nick"] = TextError.EXISTS
        }
        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }
        val userId = userRepository.save(
            UserEntity(
                nick = user.nick,
                name = null,
                bio = null,
                tel = null,
                email = null
            )
        ).userId!!
        authDetailRepository.save(
            AuthDetailEntity(
                AuthDetailEntityId(userId, AuthenticationMethod.EMAIL),
                userName
            )
        )
        passwordRepository.save(
            PasswordEntity(
                userId,
                passwordEncoder.encode(password)
            )
        )
        val authToken = jwtTokenUtil.generateToken(userId.toString())
        return AuthModel(authToken, userId)
    }

    override fun requestPasswordReset(userName: String) {
        val method = when {
            userName.matches(ValidationRules.EMAIL_REGEX.toRegex()) -> AuthenticationMethod.EMAIL
            else -> return
        }
        if (!authDetailRepository.existsByUserNameAndId_Method(userName, method)) {
            throw NoSuchElementException()
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
                userName = userName,
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
        ).orElseThrow { NoSuchElementException() }
        if (!passwordEncoder.matches(code, verificationEntity.code)) {
            throw IllegalArgumentException()
        } else if (verificationEntity.issuedAt.toKotlinInstant() + ExpirationDuration.SHORT.duration < Clock.System.now()) {
            throw ExpiredException()
        } else {
            verificationRepository.delete(verificationEntity)
            return jwtTokenUtil.generateToken(userName, ExpirationDuration.LONG)
        }
    }

    override fun existsByUserName(userName: String): Boolean {
        return authDetailRepository.existsByUserNameAndId_Method(userName, AuthenticationMethod.EMAIL)
    }

    override fun resetPassword(token: String, password: String) {
        if (!jwtTokenUtil.validateToken(token) || invalidTokenRepository.existsById(token)) {
            throw UnauthenticatedException()
        }
        val userName = jwtTokenUtil.extractSubject(token)!!
        val userId = authDetailRepository.findByUserName(userName).orElseThrow().id.userId
        passwordRepository
            .findById(userId)
            .orElseThrow()
            .copy(
                password = passwordEncoder.encode(password)
            ).let {
                passwordRepository.save(it)
            }
        invalidTokenRepository.save(InvalidTokenEntity(token))
    }
}