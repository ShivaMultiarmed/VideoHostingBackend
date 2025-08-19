package mikhail.shell.video.hosting.service

import jakarta.transaction.Transactional
import kotlinx.datetime.Clock
import kotlinx.datetime.toKotlinInstant
import mikhail.shell.video.hosting.domain.AuthModel
import mikhail.shell.video.hosting.domain.User
import mikhail.shell.video.hosting.domain.ValidationRules
import mikhail.shell.video.hosting.errors.CompoundError
import mikhail.shell.video.hosting.errors.ValidationException
import mikhail.shell.video.hosting.errors.SignInError
import mikhail.shell.video.hosting.errors.SignUpError
import mikhail.shell.video.hosting.errors.SignUpError.*
import mikhail.shell.video.hosting.repository.*
import mikhail.shell.video.hosting.repository.entities.InvalidTokenEntity
import mikhail.shell.video.hosting.repository.entities.VerificationCodePurpose
import mikhail.shell.video.hosting.repository.entities.VerificationEntity
import mikhail.shell.video.hosting.security.CryptoUtils
import mikhail.shell.video.hosting.security.JwtTokenUtil
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
        val compoundError = CompoundError<SignInError>()
        val method = if (userName.matches(emailRegex)) AuthenticationMethod.EMAIL else AuthenticationMethod.TEL
        val authDetailEntity = authDetailRepository.findByUserNameAndId_Method(userName, method).orElseThrow()
        val userId = authDetailEntity.id.userId
        val expectedPassword = passwordRepository.findById(userId).orElseThrow().password
        if (!passwordEncoder.matches(password, expectedPassword)) {
            compoundError.add(SignInError.PASSWORD_INCORRECT)
            throw ValidationException(compoundError)
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
        val compoundError = CompoundError<SignUpError>()
        if (userName.length > ValidationRules.MAX_USERNAME_LENGTH) {
            compoundError.add(SignUpError.USERNAME_TOO_LARGE)
        } else if (authDetailRepository.existsByUserNameAndId_Method(userName, AuthenticationMethod.EMAIL)) {
            compoundError.add(SignUpError.USERNAME_EXISTS)
        }
        if (compoundError.isNotEmpty()) {
            throw ValidationException(compoundError)
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
        val compoundError = CompoundError<SignUpError>()
        if (userName.length > ValidationRules.MAX_USERNAME_LENGTH) {
            compoundError.add(SignUpError.USERNAME_TOO_LARGE)
        } else if (authDetailRepository.existsByUserNameAndId_Method(userName, AuthenticationMethod.EMAIL)) {
            compoundError.add(SignUpError.USERNAME_EXISTS)
        }
        if (compoundError.isNotEmpty()) {
            throw ValidationException(compoundError)
        }
        val verificationEntity = verificationRepository
            .findByUserNameAndPurpose(
                userName = userName,
                purpose = VerificationCodePurpose.SIGNUP
            ).orElseThrow {
                IllegalArgumentException()
            }
        if (code.length != ValidationRules.CODE_LENGTH) {
            compoundError.add(SignUpError.CODE_LENGTH_NOT_CORRECT)
        } else if (!passwordEncoder.matches(code, verificationEntity.code)) {
            compoundError.add(CODE_NOT_CORRECT)
        } else if (verificationEntity.issuedAt.toKotlinInstant() + 10.minutes < Clock.System.now()) {
            compoundError.add(CODE_NOT_VALID)
        }
        if (compoundError.isNotEmpty()) {
            throw ValidationException(compoundError)
        }
        verificationRepository.delete(verificationEntity)
        return jwtTokenUtil.generateToken(userName, JwtTokenUtil.SHORT_LIVED_TOKEN_EXPIRATION_DURATION)
    }

    override fun confirmSignUpWithPassword(
        token: String,
        password: String,
        user: User
    ): AuthModel {
        val compoundError = CompoundError<SignUpError>()
        val userName = jwtTokenUtil.extractSubject(token)
        if (userName == null) {
            compoundError.add(SignUpError.USERNAME_EMPTY)
        } else if (authDetailRepository.existsByUserNameAndId_Method(userName, AuthenticationMethod.EMAIL)) {
            compoundError.add(USERNAME_EXISTS)
        }
        if (!password.matches(ValidationRules.PASSWORD_REGEX)) {
            compoundError.add(PASSWORD_NOT_VALID)
        }
        if (user.nick.length > ValidationRules.MAX_NAME_LENGTH) {
            compoundError.add(NICK_TOO_LARGE)
        } else if (userRepository.existsByNick(user.nick)) {
            compoundError.add(NICK_EXISTS)
        }
        if (compoundError.isNotEmpty()) {
            throw ValidationException(compoundError)
        }
        val createdUser = userRepository.save(user.toEntity())
        val userId = createdUser.userId!!
        val authDetailEntityId = AuthDetailEntityId(userId, AuthenticationMethod.EMAIL)
        val authDetailEntity = AuthDetailEntity(authDetailEntityId, userName!!)
        authDetailRepository.save(authDetailEntity)
        val passwordEntity = PasswordEntity(userId, passwordEncoder.encode(password))
        passwordRepository.save(passwordEntity)
        val authToken = jwtTokenUtil.generateToken(userId.toString())
        return AuthModel(authToken, userId)
    }

    override fun requestPasswordReset(userName: String) {
        val method = when {
            userName.matches(emailRegex) -> AuthenticationMethod.EMAIL
            userName.matches(telRegex) -> AuthenticationMethod.TEL
            else -> throw IllegalAccessException()
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
        ).orElseThrow()
        if (!passwordEncoder.matches(code, verificationEntity.code)) {
            throw IllegalArgumentException()
        } else if (verificationEntity.issuedAt.toKotlinInstant() + EXPIRATION_DURATION.milliseconds < Clock.System.now()) {
            throw IllegalAccessException()
        } else {
            verificationRepository.delete(verificationEntity)
            return jwtTokenUtil.generateToken(userName, EXPIRATION_DURATION)
        }
    }

    override fun resetPassword(token: String, password: String) {
        if (!jwtTokenUtil.validateToken(token)) {
            throw IllegalAccessException()
        }
        if (password.matches(ValidationRules.PASSWORD_REGEX)) {
            throw IllegalArgumentException()
        }
        val userName = jwtTokenUtil.extractSubject(token) ?: throw IllegalAccessException()
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

    private companion object {
        const val EXPIRATION_DURATION = 10 * 60 * 1000L
        val emailRegex = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\$")
        val telRegex = Regex("^\\d{8,15}\$")
    }
}