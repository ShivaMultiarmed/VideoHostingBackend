package mikhail.shell.video.hosting.service

import jakarta.transaction.Transactional
import mikhail.shell.video.hosting.domain.AuthModel
import mikhail.shell.video.hosting.domain.User
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
    fun signInWithPassword(
        userName: String,
        password: String
    ): AuthModel {
        val credentials = authRepository.findByUserNameAndId_Method(userName, AuthenticationMethod.PASSWORD).orElseThrow()
        return if (passwordEncoder.matches(password, credentials.credential)) {
            val token = jwtTokenUtil.generateToken(credentials.id.userId.toString())
            AuthModel(token, credentials.id.userId)
        } else throw Exception()
    }
    @Transactional
    fun signUpWithPassword(
        userName: String,
        password: String,
        user: User
    ): AuthModel {
        if (authRepository.existsByUserNameAndId_Method(userName, AuthenticationMethod.PASSWORD))
            throw Exception()
        val createdUser = userRepository.save(user.toEntity())
        val userId = createdUser.userId
        val credentialId = CredentialId(userId!!, AuthenticationMethod.PASSWORD)
        val credential = Credential(credentialId, passwordEncoder.encode(password), userName)
        authRepository.save(credential)
        val token = jwtTokenUtil.generateToken(userId.toString())
        return AuthModel(token, userId)
    }
}