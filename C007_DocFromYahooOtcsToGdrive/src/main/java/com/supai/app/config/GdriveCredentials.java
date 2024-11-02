package com.supai.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "gdrive.credentials")	
public class GdriveCredentials {
	private String clientId;
	private String clientSecret;
	private String grantType;
	private String refreshToken;
	private String accessToken;
}
