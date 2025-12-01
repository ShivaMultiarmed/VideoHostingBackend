package mikhail.shell.video.hosting.controllers

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import mikhail.shell.video.hosting.domain.AuthModel
import mikhail.shell.video.hosting.domain.Code
import mikhail.shell.video.hosting.domain.LongId
import mikhail.shell.video.hosting.domain.Password
import mikhail.shell.video.hosting.domain.UserCreatingModel
import mikhail.shell.video.hosting.domain.UserName
import mikhail.shell.video.hosting.domain.UserNameCheckPurpose
import mikhail.shell.video.hosting.domain.ValidEnum
import mikhail.shell.video.hosting.errors.UnauthenticatedException
import mikhail.shell.video.hosting.security.JwtTokenUtil
import mikhail.shell.video.hosting.service.AuthService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v2/auth")
class AuthController(
    private val jwtTokenUtil: JwtTokenUtil,
    private val authService: AuthService
) {

    @PostMapping("/signin/password")
    fun signInWithPassword(
        @RequestParam("user_name") @UserName userName: String,
        @RequestParam("password") @Password password: String
    ) = authService.signInWithPassword(userName = userName, password = password)

    @PostMapping("/signup/password/request")
    fun requestSignUpWithPassword(@RequestParam("user_name") @UserName userName: String?) {
        authService.requestSignUpWithPassword(userName!!)
    }

    @PostMapping("/signup/password/verification")
    fun verifySignUpWithPassword(
        @RequestParam("user_name") @UserName userName: String?,
        @RequestParam("code") @Code code: String?
    ): String {
        return authService.verifySignUpWithPassword(userName!!, code!!)
    }

    @PostMapping("/signup/password/confirm")
    fun confirmSignUpWithPassword(
        @RequestHeader("Authorization") authorization: String,
        @RequestBody @Valid user: UserCreatingRequest
    ): AuthModel {
        val token = authorization.removePrefix("Bearer ")
        if (!jwtTokenUtil.validateToken(token)) {
            throw UnauthenticatedException()
        }
        val userName = jwtTokenUtil.extractSubject(token)?: throw UnauthenticatedException()
        return authService.confirmSignUpWithPassword(
            user = UserCreatingModel(
                userName = userName,
                password = user.password!!,
                nick = user.nick!!
            )
        )
    }

    @PostMapping("/reset/password/request")
    fun requestPasswordReset(@RequestParam("user_name") @UserName userName: String?): Long {
        return authService.requestPasswordReset(userName!!)
    }

    @PostMapping("/reset/password/verification")
    fun verifyPasswordReset(
        @RequestParam("user_id") @LongId userId: Long?,
        @RequestParam("code") @Code code: String?
    ): String {
        return authService.verifyPasswordReset(
            userId = userId!!,
            code = code!!
        )
    }

    @PostMapping("/reset/password/confirmation")
    fun confirmPasswordReset(
        @RequestHeader("Authorization") authorization: String,
        @RequestParam("password") @Password password: String
    ): AuthModel {
        val token = authorization.removePrefix("Bearer ")
        if (!jwtTokenUtil.validateToken(token)) {
            throw UnauthenticatedException()
        }
        val userId = jwtTokenUtil.extractSubject(token)?.toLongOrNull()?: throw UnauthenticatedException()
        return authService.confirmPasswordReset(userId, password)
    }

    @PostMapping("/signout")
    fun signOut(
        @RequestHeader("Authorization") authorization: String
    ) {
        authService.signOut(authorization.removePrefix("Bearer "))
    }

    @GetMapping("/existence")
    fun existsByUserName(
        @RequestParam("purpose") @ValidEnum(UserNameCheckPurpose::class) purpose: String?,
        @RequestParam("user_name") @UserName userName: String?
    ) {
        authService.existsByUserName(
            purpose = UserNameCheckPurpose.valueOf(purpose!!.uppercase()),
            userName = userName!!
        )
    }
}