package com.supai.app.services.gdrive;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.supai.app.config.GdriveCredentials;
import com.supai.app.services.common.JsonObj;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AccessToken {
	private final GdriveCredentials gdriveCredentials;
	private final JsonObj jsonObj;
	private final RestTemplate restTemplate;
	private final Environment environment;

	public ResponseEntity<String> updateAccessToken() throws Exception{
		// Prepare the URL
		String url = environment.getProperty("gdrive.api.access-token");

		// Create headers and set the content type
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		// Prepare the body parameters
		MultiValueMap<String, String> bodyParams = new LinkedMultiValueMap<>();
		bodyParams.add("client_id", gdriveCredentials.getClientId());
		bodyParams.add("client_secret", gdriveCredentials.getClientSecret());
		bodyParams.add("grant_type", gdriveCredentials.getGrantType());
		bodyParams.add("refresh_token", gdriveCredentials.getRefreshToken());

		// Convert the body parameters into an HttpEntity
		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(bodyParams, headers);

		// Execute the POST request and get the response
		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

		JsonNode jsonResponse = jsonObj.getJson(response.getBody());
		gdriveCredentials.setAccessToken(jsonResponse.path("access_token").asText());

		return response;

	}

}
