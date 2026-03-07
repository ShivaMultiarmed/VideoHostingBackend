package mikhail.shell.video.hosting.service

import jakarta.transaction.Transactional
import kotlinx.datetime.Clock
import kotlinx.datetime.toKotlinInstant
import mikhail.shell.video.hosting.domain.ApplicationPaths
import mikhail.shell.video.hosting.domain.AuthModel
import mikhail.shell.video.hosting.domain.UserCreatingModel
import mikhail.shell.video.hosting.domain.UserNameCheckPurpose
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
import kotlin.io.path.Path
import kotlin.io.path.createDirectory

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
    private val invalidTokenRepository: InvalidTokenRepository,
    private val applicationPaths: ApplicationPaths
) : AuthService {

    override fun signInWithPassword(userName: String, password: String): AuthModel {
        val method = when {
            userName.matches(ValidationRules.EMAIL_REGEX.toRegex()) -> AuthenticationMethod.EMAIL
            else -> throw ValidationException(
                mapOf("userName" to TextError.PATTERN)
            )
        }
        val authDetailEntity = authDetailRepository.findByUserNameAndId_Method(userName, method).orElseThrow {
            ValidationException(
                mapOf("userName" to TextError.NOT_EXISTS)
            )
        }
        val userId = authDetailEntity.id.userId
        val expectedPassword = passwordRepository.findById(userId).orElseThrow().password
        if (!passwordEncoder.matches(password, expectedPassword)) {
            throw ValidationException(
                mapOf("password" to TextError.NOT_CORRECT)
            )
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
        val mailMessage = SimpleMailMessage().apply {
            from = noReplyEmail
            setTo(userName)
            subject = "Signing up"
            text = "Code to proceed signing up: $code"
        }
        mailSender.send(mailMessage)
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
        val verificationEntity = verificationRepository.findFirstByUserNameAndPurposeOrderByIssuedAtDesc(
            userName,
            VerificationCodePurpose.SIGN_UP
        ).orElseThrow {
            ValidationException(
                mapOf("userName" to TextError.EXISTS)
            )
        }
        if (!passwordEncoder.matches(code, verificationEntity.code)) {
            throw ValidationException(
                mapOf("code" to TextError.NOT_CORRECT)
            )
        } else if (verificationEntity.issuedAt.toKotlinInstant() + ExpirationDuration.SHORT.duration < Clock.System.now()) {
            throw ValidationException(
                mapOf("code" to TextError.NOT_VALID)
            )
        } else {
            verificationRepository.delete(verificationEntity)
            return jwtTokenUtil.generateToken(userName, ExpirationDuration.SHORT)
        }
    }

    override fun confirmSignUpWithPassword(
        user: UserCreatingModel,
        token: String
    ): AuthModel {
        if (invalidTokenRepository.existsById(token)) {
            throw UnauthenticatedException()
        }
        val userName = jwtTokenUtil.extractSubject(token) ?: throw UnauthenticatedException()
        val errors = mutableMapOf<String, Error>()
        if (authDetailRepository.existsByUserNameAndId_Method(userName, AuthenticationMethod.EMAIL)) {
            errors["userName"] = TextError.EXISTS
        }
        if (userRepository.existsByNick(user.nick)) {
            errors["nick"] = TextError.EXISTS
        }
        if (errors.isNotEmpty()) {
            throw ValidationException(errors)
        }
        invalidTokenRepository.save(InvalidTokenEntity(token))
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
                id = AuthDetailEntityId(userId, AuthenticationMethod.EMAIL),
                userName = userName
            )
        )
        passwordRepository.save(
            PasswordEntity(
                userId = userId,
                password = passwordEncoder.encode(user.password)
            )
        )
        Path(applicationPaths.USERS_BASE_PATH, userId.toString()).createDirectory()
        val authToken = jwtTokenUtil.generateToken(userId.toString())
        return AuthModel(authToken, userId)
    }

    override fun requestPasswordReset(userName: String): Long {
        val method = when {
            userName.matches(ValidationRules.EMAIL_REGEX.toRegex()) -> AuthenticationMethod.EMAIL
            else -> throw ValidationException(
                mapOf("userName" to TextError.PATTERN)
            )
        }
        val authEntity = authDetailRepository.findByUserNameAndId_Method(userName, method).orElseThrow {
            ValidationException(
                mapOf("userName" to TextError.NOT_EXISTS)
            )
        }
        val resetCode = cryptoUtils.generateString(4)
        val mailMessage = SimpleMailMessage().apply {
            from = noReplyEmail
            setTo(userName)
            subject = "Password recovery"
            text = "Your password recovery code: $resetCode"
        }
        mailSender.send(mailMessage)
        verificationRepository.save(
            VerificationEntity(
                userName = authEntity.id.userId.toString(),
                issuedAt = Instant.now(),
                purpose = VerificationCodePurpose.RESET,
                code = passwordEncoder.encode(resetCode)
            )
        )
        return authEntity.id.userId
    }

    override fun verifyPasswordReset(
        userId: Long,
        code: String
    ): String {
        val verificationEntity = verificationRepository.findFirstByUserNameAndPurposeOrderByIssuedAtDesc(
            userName = userId.toString(),
            purpose = VerificationCodePurpose.RESET
        ).orElseThrow {
            ValidationException(
                mapOf("userId" to NumericError.NOT_EXISTS)
            )
        }
        if (!passwordEncoder.matches(code, verificationEntity.code)) {
            throw ValidationException(
                mapOf("code" to TextError.NOT_CORRECT)
            )
        } else if (verificationEntity.issuedAt.toKotlinInstant() + ExpirationDuration.SHORT.duration < Clock.System.now()) {
            throw ValidationException(
                mapOf("code" to TextError.NOT_VALID)
            )
        } else {
            verificationRepository.delete(verificationEntity)
            return jwtTokenUtil.generateToken(userId.toString(), ExpirationDuration.SHORT)
        }
    }

    override fun confirmPasswordReset(
        token: String,
        password: String
    ): AuthModel {
        if (invalidTokenRepository.existsById(token)) {
            throw UnauthenticatedException()
        }
        val userId = jwtTokenUtil.extractSubject(token)?.toLongOrNull() ?: throw UnauthenticatedException()
        val authEntity = authDetailRepository
            .findById(AuthDetailEntityId(userId, AuthenticationMethod.EMAIL))
            .orElseThrow {
                ValidationException(
                    mapOf("userId" to NumericError.NOT_EXISTS)
                )
            }
        val passwordEntity = passwordRepository.findById(authEntity.id.userId).orElseThrow {
            ValidationException(
                mapOf("userId" to NumericError.NOT_EXISTS)
            )
        }.copy(password = passwordEncoder.encode(password))
        invalidTokenRepository.save(InvalidTokenEntity(token))
        passwordRepository.save(passwordEntity)
        val authToken = jwtTokenUtil.generateToken(userId.toString())
        return AuthModel(authToken, authEntity.id.userId)
    }

    override fun existsByUserName(
        purpose: UserNameCheckPurpose,
        userName: String
    ) {
        val exists = authDetailRepository.existsByUserName(userName)
        when (purpose) {
            UserNameCheckPurpose.SIGN_UP -> {
                if (exists) {
                    throw UniquenessViolationException()
                }
            }

            UserNameCheckPurpose.SIGN_IN, UserNameCheckPurpose.RESET -> {
                if (!exists) {
                    throw NoSuchElementException()
                }
            }
        }
    }
    private companion object {
        const val noReplyEmail = "no-reply@trendy-app.ru"
    }
}