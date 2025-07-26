package mikhail.shell.video.hosting.controllers

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import java.io.File

@Controller
@RequestMapping("/docs")
class DocumentController {
    @Value("\${hosting.android.domain.verification.url}")
    private lateinit var androidDomainVerificationFilePath: String
    @GetMapping("/privacy-policy")
    fun providePrivacyPolicy(): String {
        return "docs/PrivacyPolicy"
    }
    @GetMapping("/account-deletion")
    fun provideAccountDeletionInstruction(): String {
        return "docs/AccountDeletion"
    }
    @GetMapping("/standards-against-csae")
    fun provideStandardsAgainstCsae(): String {
        return "docs/StandardsAgainstCsae"
    }
    @GetMapping("/android-domain-verification", produces = ["application/json"])
    @ResponseBody
    fun verifyDomainPossessing(): String {
        return File(androidDomainVerificationFilePath).readText()
    }
}
