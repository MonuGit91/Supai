package com.supai.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "otcs.category")
public class OtcsCategory {
	private Map<String, String> attr;
    private Map<String, String> spendCategory;
}