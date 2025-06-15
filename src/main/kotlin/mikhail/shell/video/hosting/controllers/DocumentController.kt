package mikhail.shell.video.hosting.controllers

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody
import java.io.File

@Controller
class DocumentController {
    @Value("\${hosting.android.domain.verification.url}")
    private lateinit var androidDomainVerificationFilePath: String
    @GetMapping("/docs/privacy-policy")
    fun providePrivacyPolicy(): String {
        return "docs/PrivacyPolicy"
    }
    @GetMapping("/docs/account-deletion")
    fun provideAccountDeletionInstruction(): String {
        return "docs/AccountDeletion"
    }
    @GetMapping("/.well-known/assetlinks.json")
    @ResponseBody
    fun verifyDomainPossessing(): String {
        return File(androidDomainVerificationFilePath).readText()
    }
}