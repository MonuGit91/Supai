package com.supai.app.services.otcs;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.supai.app.config.OtcsCredentials;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DownloadDoc {
	private final RestTemplate restTemplate;
	private final OtcsCredentials otcsCredentials;
	private final Environment environment;

	public ResponseEntity<byte[]> GetDoc(String id, String name) throws Exception {
		
		String url = environment.getProperty("otcs.api.base-url") + environment.getProperty("otcs.api.get-doc").replace("{id}", id).replace("{name}", name);

		// Create headers and add the otcsticket
		HttpHeaders headers = new HttpHeaders();
		headers.set("otcsticket", otcsCredentials.getOtcsticket());

		// Create HttpEntity with headers
		HttpEntity<String> entity = new HttpEntity<>(headers);

		// Make the GET request and get the response as a byte array
		ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);

		return response;
	}
}
