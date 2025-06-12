package mikhail.shell.video.hosting.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/docs")
class DocumentController {
    @GetMapping("/privacy-policy")
    fun providePrivacyPolicy(): String {
        return "docs/PrivacyPolicy"
    }
    @GetMapping("account-deletion")
    fun provideAccountDeletionInstruction(): String {
        return "docs/AccountDeletion"
    }
}