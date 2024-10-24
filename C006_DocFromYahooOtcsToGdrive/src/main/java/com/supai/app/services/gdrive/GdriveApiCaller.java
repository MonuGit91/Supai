package com.supai.app.services.gdrive;

import java.net.URI;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.supai.app.config.GdriveApiConfig;
import com.supai.app.config.GdriveCredentials;
import com.supai.app.services.common.JsonObj;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@RequiredArgsConstructor
public class GdriveApiCaller {
	private static final Logger log = LoggerFactory.getLogger(JsonObj.class);
	
	private final JsonObj jsonObj;
	private final RestTemplate restTemplate;
	private final GdriveCredentials gdriveCredentials;
	private final GdriveApiConfig gdriveApiConfig;
	private final ObjectMapper objectMapper;

	public JsonNode listSubFolders(String rootFolderId) throws Exception {
		// Build the query parameters safely using UriComponentsBuilder
		String url = gdriveApiConfig.getListSubfolders();// "https://www.googleapis.com/drive/v3/files";
		String baseQueryParam = "mimeType='application/vnd.google-apps.folder' and 'folderId' in parents and trashed=false";

		URI finalUrl = UriComponentsBuilder.fromHttpUrl(url)
				.queryParam("q", baseQueryParam.replace("folderId", rootFolderId))
				.queryParam("fields", "files(id,name)").build().toUri();

		// Execute the GET request
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(gdriveCredentials.getAccessToken());

		// Create an entity with the headers
		HttpEntity<String> entity = new HttpEntity<>(headers);

		ResponseEntity<String> response = restTemplate.exchange(finalUrl, HttpMethod.GET, entity, String.class);

		// Return the response body as a String
		return jsonObj.getJson(response.getBody());
	}

	public JsonNode createFolder(String folderName, String rootFolderId) throws Exception {
		String url = gdriveApiConfig.getCreateFolder();// "https://www.googleapis.com/drive/v3/files";

		// Build JSON request body using ObjectNode
		ObjectNode requestBody = objectMapper.createObjectNode();
		requestBody.put("name", folderName);
		requestBody.put("mimeType", "application/vnd.google-apps.folder");
		requestBody.putArray("parents").add(rootFolderId);

		// Prepare headers
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(gdriveCredentials.getAccessToken()); // Bearer token for Authorization

		HttpEntity<JsonNode> requestEntity = new HttpEntity<>(requestBody, headers);
		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

		// Return the parsed JSON response
		return jsonObj.getJson(response.getBody()); // The response body contains the created folder details
	}

	public JsonNode uploadFileContent(String uploadUrl, ResponseEntity<byte[]> response) throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		headers.setBearerAuth(gdriveCredentials.getAccessToken()); // Bearer token for Authorization

		// Prepare the file data from ResponseEntity<byte[]>
		byte[] fileContent = response.getBody();

		HttpEntity<byte[]> requestEntity = new HttpEntity<>(fileContent, headers);

		ResponseEntity<String> uploadResponse = restTemplate.exchange(uploadUrl, HttpMethod.PUT, requestEntity,
				String.class);

		if (uploadResponse.getStatusCode() == HttpStatus.OK || uploadResponse.getStatusCode() == HttpStatus.CREATED) {
			log.info("File uploaded successfully.");
			return jsonObj.getJson(uploadResponse.getBody());
		} else {
			log.info("Failed to upload the file. Status code: " + uploadResponse.getStatusCode());
			return null;
		}
	}

	public String initiateResumableUpload(String fileName, String parentId) throws Exception {

		String url = gdriveApiConfig.getResumableUpload();// "https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable";
		String uploadUrl = null;

		// Prepare metadata for the file to be uploaded
		ObjectNode metadata = objectMapper.createObjectNode();
		metadata.put("name", fileName);
		metadata.put("mimeType", "application/octet-stream"); // Update based on your file type
		metadata.putArray("parents").add(parentId);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(gdriveCredentials.getAccessToken()); // Bearer token for Authorization
		headers.set("X-Upload-Content-Type", "application/octet-stream");

		HttpEntity<JsonNode> requestEntity = new HttpEntity<>(metadata, headers);

		ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

		// The location header contains the upload URL
		if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
			uploadUrl = response.getHeaders().getLocation().toString();
			return uploadUrl;
		} else {
			return null;
		}
	}

	public boolean isDocPresentOnGdrive(String docName, String folderId) {
		// TODO Auto-generated method stub
		// Retrieve base URL from config
		String url = gdriveApiConfig.getListSubfolders(); // "https://www.googleapis.com/drive/v3/files"

		// Create the query to search for the document by name in the specified folder
		String queryParam = "name='" + docName + "' and '" + folderId + "' in parents and trashed=false";

		// Build the final URL using UriComponentsBuilder
		URI finalUrl = UriComponentsBuilder.fromHttpUrl(url).queryParam("q", queryParam)
				.queryParam("fields", "files(id,name)") // Requesting specific fields: file ID and name
				.build().toUri();

		// Prepare headers (add Authorization Bearer token)
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(gdriveCredentials.getAccessToken()); // Replace with actual access token

		// Create HttpEntity containing headers
		HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

		// Call the API
		ResponseEntity<String> response = restTemplate.exchange(finalUrl, HttpMethod.GET, requestEntity, String.class);

		// Check the response and determine if the document exists
		if (response.getStatusCode().is2xxSuccessful()) {
			// Parse the response body
			String responseBody = response.getBody();
			// Check if the document is present in the response
			return responseBody != null && responseBody.contains(docName);
		} else {
			log.error("Failed to check document presence: " + response.getStatusCode());
			return false; // Return false if the API call was unsuccessful
		}

	}

}
