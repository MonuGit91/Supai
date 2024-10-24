package com.supai.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@ConfigurationProperties(prefix = "gdrive.api")
@Data
public class GdriveApiConfig {

    private String accessToken;
    private String resumableUpload;
    private String listSubfolders;
    private String createFolder;
}