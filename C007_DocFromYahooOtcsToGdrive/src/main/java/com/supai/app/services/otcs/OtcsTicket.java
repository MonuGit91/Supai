package com.supai.app.services.otcs;

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
import com.supai.app.config.OtcsApi;
import com.supai.app.config.OtcsCredentials;
import com.supai.app.services.common.JsonObj;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OtcsTicket {
	private final RestTemplate restTemplate;
	private final OtcsCredentials otcsCredentials;
	private final JsonObj jsonObj;
	private final OtcsApi otcsApi;
	
	public ResponseEntity<String> updateOtcsToken() throws Exception{
		String url = otcsApi.getBaseUrl() + otcsCredentials.getAuthApi();
		
		HttpHeaders header = new HttpHeaders();
		header.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		
		MultiValueMap<String, String> bodyParam = new LinkedMultiValueMap<>();
		bodyParam.add("username", otcsCredentials.getUsername());
		bodyParam.add("password", otcsCredentials.getPassword());
		
		HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(bodyParam, header);
		
		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, httpEntity, String.class);
		
		JsonNode jsonResponse = jsonObj.getJson(response.getBody());
		otcsCredentials.setOtcsticket(jsonResponse.get("ticket").asText());
		return response;
	}
}
