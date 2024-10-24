package com.supai.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@ConfigurationProperties(prefix = "otcs.api")
@Data
public class OtcsApi {
	private String nodeProperty;
	private String getDoc;
	private String searchDocs;
	private String nodeCategory;
}
