package mikhail.shell.video.hosting.controllers

import jakarta.servlet.http.HttpServletRequest
import org.springframework.boot.webmvc.error.ErrorController
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class MainErrorController : ErrorController {
    @RequestMapping("/error")
    fun handleError(request: HttpServletRequest): ResponseEntity<Unit> {
        val status = (request.getAttribute("jakarta.servlet.error.status_code") as? Int)?: 500
        return ResponseEntity.status(status).build()
    }
}