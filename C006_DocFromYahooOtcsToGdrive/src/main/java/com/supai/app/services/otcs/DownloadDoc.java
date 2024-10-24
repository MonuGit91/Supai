package com.supai.app.services.otcs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.supai.app.config.OtcsCredentials;
import com.supai.app.services.common.JsonObj;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class DownloadDoc {
	private static final Logger log = LoggerFactory.getLogger(JsonObj.class);
	@Autowired
	private RestTemplate restTemplate;
	@Autowired
	private OtcsCredentials otcsCredentials;
	@Autowired
	private Environment environment;

	public ResponseEntity<byte[]> GetDoc(int id, String name) throws Exception {

		String url = environment.getProperty("otcs.api.get-doc").replace("{id}", "" + id).replace("{name}", name);

		// Create headers and add the otcsticket
		HttpHeaders headers = new HttpHeaders();
		headers.set("otcsticket", otcsCredentials.getOtcsticket());

		// Create HttpEntity with headers
		HttpEntity<String> entity = new HttpEntity<>(headers);

		// Make the GET request and get the response as a byte array
		ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);

		if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
			// Path to save the file
			log.info("downloading is successfull. Status: " + response.getStatusCode());
			return response;
		} else {
			log.info("Failed to download the document. Status code: " + response.getStatusCode());
			return null;
		}

	}
}
