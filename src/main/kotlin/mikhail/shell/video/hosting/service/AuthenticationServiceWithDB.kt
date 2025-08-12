package mikhail.shell.video.hosting.service

import jakarta.transaction.Transactional
import mikhail.shell.video.hosting.domain.AuthModel
import mikhail.shell.video.hosting.domain.User
import mikhail.shell.video.hosting.domain.ValidationRules
import mikhail.shell.video.hosting.errors.CompoundError
import mikhail.shell.video.hosting.errors.ValidationException
import mikhail.shell.video.hosting.errors.SignInError
import mikhail.shell.video.hosting.errors.SignUpError
import mikhail.shell.video.hosting.errors.SignUpError.*
import mikhail.shell.video.hosting.repository.*
import mikhail.shell.video.hosting.repository.entities.RecoveryEntity
import mikhail.shell.video.hosting.security.CryptoUtils
import mikhail.shell.video.hosting.security.JwtTokenUtil
import org.springframework.mail.MailSender
import org.springframework.mail.SimpleMailMessage
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class AuthenticationServiceWithDB(
    private val jwtTokenUtil: JwtTokenUtil,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val mailSender: MailSender,
    private val cryptoUtils: CryptoUtils,
    private val recoveryRepository: RecoveryRepository
) : AuthenticationService {
    override fun signInWithPassword(
        userName: String,
        password: String
    ): AuthModel {
        val compoundError = CompoundError<SignInError>()
        val credentials =
            authRepository.findByUserNameAndId_Method(userName, AuthenticationMethod.PASSWORD).orElseThrow()
        if (!passwordEncoder.matches(password, credentials.credential)) {
            compoundError.add(SignInError.PASSWORD_INCORRECT)
        } else {
            val token = jwtTokenUtil.generateToken(credentials.id.userId.toString())
            return AuthModel(token, credentials.id.userId)
        }
        throw ValidationException(compoundError)
    }

    override fun signOut(token: String) {

    }

    @Transactional
    override fun signUpWithPassword(
        userName: String,
        password: String,
        user: User?
    ): AuthModel {
        val compoundError = CompoundError<SignUpError>()
        if (userName.length > ValidationRules.MAX_USERNAME_LENGTH) {
            compoundError.add(USERNAME_TOO_LARGE)
        } else if (authRepository.existsByUserNameAndId_Method(userName, AuthenticationMethod.PASSWORD)) {
            compoundError.add(USERNAME_EXISTS)
        }
        val passwordLengthRange = ValidationRules.MIN_PASSWORD_LENGTH..ValidationRules.MAX_PASSWORD_LENGTH
        if (password.length !in passwordLengthRange) {
            compoundError.add(PASSWORD_NOT_VALID)
        }
        if ((user?.nick?.length ?: 0) > ValidationRules.MAX_NAME_LENGTH) {
            compoundError.add(NICK_TOO_LARGE)
        } else if (userRepository.existsByNick(user!!.nick)) {
            compoundError.add(NICK_EXISTS)
        }
        if (compoundError.isNotEmpty()) {
            throw ValidationException(compoundError)
        }
        val createdUser = userRepository.save(user!!.toEntity())
        val userId = createdUser.userId
        val credentialId = CredentialId(userId!!, AuthenticationMethod.PASSWORD)
        val credential = Credential(credentialId, userName, passwordEncoder.encode(password))
        authRepository.save(credential)
        val token = jwtTokenUtil.generateToken(userId.toString())
        return AuthModel(token, userId)
    }

    override fun requestPasswordReset(userName: String) {
        val userId = authRepository
            .findByUserNameAndId_Method(
                userName = userName,
                method = AuthenticationMethod.PASSWORD
            )
            .orElseThrow().id.userId
        val resetCode = cryptoUtils.generateString(4)
        SimpleMailMessage().apply {
            setTo(userName)
            from = "trendy@no-reply.com"
            subject = "Password recovery"
            text = "Your password recovery password: $resetCode"
            mailSender.send(this)
        }
        recoveryRepository.save(
            RecoveryEntity(
                userId = userId,
                dateTime = LocalDateTime.now(),
                code = passwordEncoder.encode(resetCode)
            )
        )
    }

    override fun verifyPasswordReset(userId: Long, resetCode: String): String {
        val recoveryEntity = recoveryRepository.findByUserId(userId).orElseThrow()
        if (!passwordEncoder.matches(resetCode, recoveryEntity.code)) {
            throw IllegalArgumentException()
        } else if (recoveryEntity.dateTime < LocalDateTime.now()) {
            throw IllegalAccessException()
        } else {
            val userName = authRepository
                .findById(
                    CredentialId(
                        userId = userId,
                        method = AuthenticationMethod.PASSWORD
                    )
                )
                .orElseThrow()
                .userName
            return jwtTokenUtil.generateToken(userName, RECOVERY_EXPIRATION_DURATION)
        }
    }

    override fun resetPassword(resetToken: String, password: String) {
        if (!jwtTokenUtil.validateToken(resetToken)) {
            throw IllegalAccessException()
        }
        val passwordLengthRange = ValidationRules.MIN_PASSWORD_LENGTH..ValidationRules.MAX_PASSWORD_LENGTH
        if (password.length !in passwordLengthRange) {
            throw IllegalArgumentException()
        }
        val userId = jwtTokenUtil.extractUserId(resetToken) ?: throw IllegalAccessException()
        authRepository
            .findById(
                CredentialId(
                    userId = userId,
                    method = AuthenticationMethod.PASSWORD
                )
            )
            .orElseThrow()
            .copy(credential = passwordEncoder.encode(password))
            .also { authRepository.save(it) }
    }

    private companion object {
        const val RECOVERY_EXPIRATION_DURATION = 10 * 60 * 1000L
    }
}