package mikhail.shell.video.hosting.service

import jakarta.transaction.Transactional
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
        val method = when {
            userName.matches(ValidationRules.EMAIL_REGEX.toRegex()) -> AuthenticationMethod.EMAIL
            else -> throw IllegalArgumentException()
        }
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
        val verificationEntity = verificationRepository.findLastByUserNameAndPurposeOrderByIssuedAtDesc(
            userName,
            VerificationCodePurpose.SIGN_UP
        ).orElseThrow()
        if (!passwordEncoder.matches(code, verificationEntity.code)) {
            throw ValidationException(
                mapOf("code" to TextError.NOT_CORRECT)
            )
        }
        verificationRepository.delete(verificationEntity)
        return jwtTokenUtil.generateToken(userName, ExpirationDuration.SHORT)
    }

    override fun confirmSignUpWithPassword(user: UserCreatingModel): AuthModel {
        val errors = mutableMapOf<String, Error>()
        if (authDetailRepository.existsByUserNameAndId_Method(user.userName, AuthenticationMethod.EMAIL)) {
            errors["userName"] = TextError.EXISTS
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
                id = AuthDetailEntityId(userId, AuthenticationMethod.EMAIL),
                userName = user.userName
            )
        )
        passwordRepository.save(
            PasswordEntity(
                userId = userId,
                password = passwordEncoder.encode(user.password)
            )
        )
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
        SimpleMailMessage().apply {
            setTo(userName)
            from = "trendy@no-reply.com"
            subject = "Password recovery"
            text = "Your password recovery code: $resetCode"
            mailSender.send(this)
        }
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
        val verificationEntity = verificationRepository.findLastByUserNameAndPurposeOrderByIssuedAtDesc(
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
        }
//        else if (verificationEntity.issuedAt.toKotlinInstant() + ExpirationDuration.SHORT.duration < Clock.System.now()) {
//            throw ExpiredException()
//        }
        else {
            verificationRepository.delete(verificationEntity)
            return jwtTokenUtil.generateToken(userId.toString(), ExpirationDuration.SHORT)
        }
    }

    override fun existsByUserName(userName: String): Boolean {
        return authDetailRepository.existsByUserNameAndId_Method(userName, AuthenticationMethod.EMAIL)
    }

    override fun confirmPasswordReset(
        userId: Long,
        password: String
    ): AuthModel {
        val authEntity = authDetailRepository
            .findById(AuthDetailEntityId(userId, AuthenticationMethod.EMAIL))
            .orElseThrow {
                ValidationException(
                    mapOf("userId" to NumericError.NOT_EXISTS)
                )
            }
        val passwordEntity = passwordRepository.findById(authEntity.id.userId).orElseThrow {
            ValidationException(
                mapOf("userName" to NumericError.NOT_EXISTS)
            )
        }.copy(password = passwordEncoder.encode(password))
        passwordRepository.save(passwordEntity)
        val token = jwtTokenUtil.generateToken(userId.toString())
        return AuthModel(token, authEntity.id.userId)
    }
}