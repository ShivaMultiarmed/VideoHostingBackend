package mikhail.shell.video.hosting.service

import mikhail.shell.video.hosting.repository.AuthRepository
import mikhail.shell.video.hosting.repository.AuthenticationMethod
import mikhail.shell.video.hosting.repository.CredentialId
import mikhail.shell.video.hosting.repository.UserRepository
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
    fun validateUsernameAndPassword(
        username: String,
        password: String
    ): String {
        val userId = userRepository.findByName(username).orElseThrow().userId
        val id = CredentialId(userId, AuthenticationMethod.PASSWORD)
        val credentials = authRepository.findById(id).orElseThrow()
        if (passwordEncoder.matches(password, credentials.credential)) {
            return jwtTokenUtil.generateToken(username)
        } else throw Exception()
    }
}