package com.supai.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "otcs.credentials")
@Component
public class OtcsCredentials {
    private String authApi;
    private String username;
    private String password;
    private String otcsticket;
}