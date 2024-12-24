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
    private val passwordEncoder: PasswordEncoder
) {
    fun validateUsernameAndPassword(
        userName: String,
        password: String
    ): String {
        val credentials = authRepository.findByUserNameAndId_Method(userName, AuthenticationMethod.PASSWORD).orElseThrow()
        if (passwordEncoder.matches(password, credentials.credential)) {
            return jwtTokenUtil.generateToken(credentials.id.userId.toString())
        } else throw Exception()
    }
}