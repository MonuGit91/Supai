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
public class OtcsNode {
	@Autowired
	private RestTemplate restTemplate;
	@Autowired
	private JsonObj jsonObj;
	@Autowired
	private OtcsCredentials otcsCredentials;
	@Autowired
	private Environment environment;
	
	public JsonNode getNodeProperty(int id) throws Exception{

		HttpHeaders headers = new HttpHeaders();
		headers.set("otcsticket", otcsCredentials.getOtcsticket());

		HttpEntity<String> httpEntity = new HttpEntity<>(headers);

		String url = environment.getProperty("otcs.api.node-property").replace("{id}", ""+id);
		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class);

		// Parse the response into a JsonNode to preserve the structure
		JsonNode jsonNode = jsonObj.getJson(response.getBody());

		return jsonNode; // Return the JsonNode to the client
	}
}
