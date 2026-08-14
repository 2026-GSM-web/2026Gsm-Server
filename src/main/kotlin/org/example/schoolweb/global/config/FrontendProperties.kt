package org.example.schoolweb.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.frontend")
data class FrontendProperties(
    val oauth2RedirectUri: String,
    val oauth2ErrorRedirectUri: String
)
