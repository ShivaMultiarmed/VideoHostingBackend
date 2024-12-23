package mikhail.shell.video.hosting.controllers

import mikhail.shell.video.hosting.service.AuthService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthenticationController(
    private val authService: AuthService
) {
    @PostMapping("/signin/password")
    fun signInWithPassword(
        @RequestParam username: String,
        @RequestParam password: String
    ): String {
        return authService.validateUsernameAndPassword(username, password)
    }
}