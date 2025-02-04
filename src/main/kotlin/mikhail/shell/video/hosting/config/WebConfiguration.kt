//package mikhail.shell.video.hosting.config
//
//import org.apache.catalina.connector.Connector
//import org.apache.catalina.security.SecurityConstraint
//import org.apache.catalina.security.SecurityCollection
//import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
//import org.springframework.context.annotation.Bean
//import org.springframework.context.annotation.Configuration
//import org.springframework.security.config.annotation.web.builders.HttpSecurity
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
//
//@Configuration
//@EnableWebSecurity
//class WebConfiguration {
//    private val httpsPort = 9999
//    private val httpPort = 10000
//    @Bean
//    fun servletContainer(): TomcatServletWebServerFactory {
//        return TomcatServletWebServerFactory().apply {
//            addContextCustomizers { context ->
//                val securityConstraint = SecurityConstraint().apply {
//                    userConstraint = "CONFIDENTIAL"
//                }
//                securityConstraint.addCollection(SecurityCollection().apply {
//                    addPattern("/*")
//                })
//                context.addConstraint(securityConstraint)
//            }
//            addAdditionalTomcatConnectors(httpConnector())
//        }
//    }
//    private fun httpConnector(): Connector {
//        return Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL).apply {
//            scheme = "http"
//            port = httpPort
//            secure = false
//            redirectPort = httpsPort
//        }
//    }
//}