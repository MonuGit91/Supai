package com.supai.app.services.otcs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.supai.app.config.OtcsCredentials;
import com.supai.app.services.common.JsonObj;

@Component
public class OtcsApiCall {
	@Autowired
	private RestTemplate restTemplate;
	@Autowired
	private JsonObj jsonObj;
	@Autowired
	private OtcsCredentials otcsCredentials;
	@Autowired
	private Environment environment;

	public JsonNode getCategoryDetails(String nodeId, String categoryId) throws Exception{
		String url = environment.getProperty("otcs.api.node-category").replace("nodeId", nodeId).replace("categoryId", categoryId);

		// Set up headers
		HttpHeaders headers = new HttpHeaders();
		headers.set("otcsticket", otcsCredentials.getOtcsticket());

		// Create an entity with the headers
		HttpEntity<String> entity = new HttpEntity<>(headers);

		// Make the GET request and capture the response
		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
		return jsonObj.getJson(response.getBody());
	}
}
