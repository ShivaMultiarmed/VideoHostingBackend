package mikhail.shell.video.hosting.service

import jakarta.transaction.Transactional
import mikhail.shell.video.hosting.domain.AuthModel
import mikhail.shell.video.hosting.domain.User
import mikhail.shell.video.hosting.errors.CompoundError
import mikhail.shell.video.hosting.errors.HostingDataException
import mikhail.shell.video.hosting.errors.SignInError
import mikhail.shell.video.hosting.errors.SignUpError
import mikhail.shell.video.hosting.errors.SignUpError.*
import mikhail.shell.video.hosting.repository.*
import mikhail.shell.video.hosting.security.JwtTokenUtil
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService (
    private val jwtTokenUtil: JwtTokenUtil,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {
    companion object {
        val emailRegex = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\$")
    }
    fun signInWithPassword(
        userName: String?,
        password: String?
    ): AuthModel {
        val compoundError = CompoundError<SignInError>()
        lateinit var credentials: Credential
        if (userName == null || userName == "") {
            compoundError.add(SignInError.EMAIL_EMPTY)
        } else if (!emailRegex.matches(userName)) {
            compoundError.add(SignInError.EMAIL_INVALID)
        } else if (!authRepository.existsByUserNameAndId_Method(userName, AuthenticationMethod.PASSWORD)) {
            compoundError.add(SignInError.EMAIL_NOT_FOUND)
        }
        if (password == null || password == "") {
            compoundError.add(SignInError.PASSWORD_EMPTY)
        } else if (authRepository.existsByUserNameAndId_Method(userName!!, AuthenticationMethod.PASSWORD)) {
            credentials = authRepository.findByUserNameAndId_Method(userName, AuthenticationMethod.PASSWORD).get()
            if (!passwordEncoder.matches(password, credentials.credential)) {
                compoundError.add(SignInError.PASSWORD_INCORRECT)
            }
        }
        if (compoundError.isNotNull()) {
            throw HostingDataException(compoundError)
        }
        val token = jwtTokenUtil.generateToken(credentials.id.userId.toString())
        return AuthModel(token, credentials.id.userId)
    }
    @Transactional
    fun signUpWithPassword(
        userName: String?,
        password: String?,
        user: User?
    ): AuthModel {
        val compoundError = CompoundError<SignUpError>()

        if (userName == null || userName == "")
            compoundError.add(EMAIL_EMPTY)
        if (password == null || password == "")
            compoundError.add(PASSWORD_EMPTY)
        if (user == null)
            compoundError.add(UNEXPECTED)
        if (authRepository.existsByUserNameAndId_Method(userName!!, AuthenticationMethod.PASSWORD))
            compoundError.add(EMAIL_EXISTS)

        if (compoundError.isNotNull())
            throw HostingDataException(compoundError)

        val createdUser = userRepository.save(user!!.toEntity())
        val userId = createdUser.userId
        val credentialId = CredentialId(userId!!, AuthenticationMethod.PASSWORD)
        val credential = Credential(credentialId, passwordEncoder.encode(password), userName)
        authRepository.save(credential)
        val token = jwtTokenUtil.generateToken(userId.toString())
        return AuthModel(token, userId)
    }
}