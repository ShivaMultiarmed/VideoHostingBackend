package mikhail.shell.video.hosting.security

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority

class HostingToken(
    private val principal: Any?,
    private val credentials: Any?,
    authorities: Collection<GrantedAuthority>? = null
) : AbstractAuthenticationToken(authorities) {
    init {
        isAuthenticated = principal != null
    }

    override fun getCredentials() = credentials

    override fun getPrincipal() = principal
}