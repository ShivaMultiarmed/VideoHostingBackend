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
import mikhail.shell.video.hosting.security.JwtTokenUtil
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthenticationService (
    private val jwtTokenUtil: JwtTokenUtil,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {
    fun signInWithPassword(
        userName: String,
        password: String
    ): AuthModel {
        val compoundError = CompoundError<SignInError>()
        val credentials = authRepository.findByUserNameAndId_Method(userName, AuthenticationMethod.PASSWORD).orElseThrow()
        if (!passwordEncoder.matches(password, credentials.credential)) {
            compoundError.add(SignInError.PASSWORD_INCORRECT)
        } else {
            val token = jwtTokenUtil.generateToken(credentials.id.userId.toString())
            return AuthModel(token, credentials.id.userId)
        }
        throw ValidationException(compoundError)
    }
    @Transactional
    fun signUpWithPassword(
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
        val passwordLengthRange = ValidationRules.MIN_PASSWORD_LENGTH .. ValidationRules.MAX_PASSWORD_LENGTH
        if (password.length !in passwordLengthRange) {
            compoundError.add(PASSWORD_NOT_VALID)
        }
        if ((user?.nick?.length ?: 0) > ValidationRules.MAX_NAME_LENGTH) {
            compoundError.add(NICK_TOO_LARGE)
        } else if (userRepository.existsByNick(user!!.nick)) {
            compoundError.add(NICK_EXISTS)
        }
        if (compoundError.isNotNull()) {
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
}