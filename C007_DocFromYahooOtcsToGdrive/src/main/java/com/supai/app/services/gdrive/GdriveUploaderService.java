package com.supai.app.services.gdrive;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.supai.app.config.OtcsCategory;
import com.supai.app.services.common.JsonObj;
import com.supai.app.services.otcs.DownloadDoc;
import com.supai.app.services.otcs.OtcsTicket;
import com.supai.app.services.otcs.SearchDocs;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GdriveUploaderService {
	private static final Logger log = LoggerFactory.getLogger(JsonObj.class);
	private final SearchDocs searchDocs;
	private final DownloadDoc downloadDoc;
	private final Environment environment;
	private final GdriveApiCaller gdriveApiCaller;
	private final AccessToken accessToken;
	private final OtcsTicket otcsTicket;
	private final OtcsCategory otcsCategory;
	private final JsonObj jsonObj;

	public void upload() {

		ResponseEntity<String> searchResponse = retryIfUnauthorizedOtcs(() -> searchDocs.searchAllDocs(),
				"Faild: failed to search docs", "Success: searching docs successfull");
		
		if ((searchResponse == null) || !searchResponse.hasBody() || !jsonObj.getJson(searchResponse.getBody()).has("results")) {
			return;
		}
		
		JsonNode jsonResponse = jsonObj.getJson(searchResponse.getBody());
		JsonNode resultsNode = jsonResponse.path("results");

		if (resultsNode.isArray()) {
			for (JsonNode result : resultsNode) {
				JsonNode propertiesNode = result.path("data").path("properties");
				JsonNode regionsNode = result.path("data").path("regions");

				if (propertiesNode.isMissingNode() || regionsNode.isMissingNode())
					continue;

				String docId = propertiesNode.path("id").asText();
				String docName = propertiesNode.path("name").asText();
				String spendCategory = regionsNode.path(otcsCategory.getAttr()).asText();

				ResponseEntity<byte[]> responseDoc = retryIfUnauthorizedOtcs(() -> downloadDoc.GetDoc(docId, docName),
						"Faild: downloading doc: " + docName, "Success: Document downloaded successfully: " + docName);
				if (responseDoc != null) {
					uploadToGdrive(responseDoc, spendCategory, docName);
				}
			}
		} else {
			log.info("Faild: No results found for query_id :  {} and attribute: {}", otcsCategory.getQueryId(),
					otcsCategory.getAttr());
		}
	}

	private void uploadToGdrive(ResponseEntity<byte[]> response, String spendCategory, String docName) {

		String rootFolderIdInGdrive = environment.getProperty("gdrive.folder.root-Supai");
		String spendCategoryIdInGdrive = getFolderIdFromGdrive(spendCategory, rootFolderIdInGdrive);

		ResponseEntity<String> responseIsDocExist = retryIfUnauthorizedGdrive(
				() -> gdriveApiCaller.isDocPresentOnGdrive(docName, spendCategoryIdInGdrive),
				"Faild: checking doc: " + docName + " exist or not",
				"Success: checking doc: " + docName + " exist or not");
		if (responseIsDocExist == null || responseIsDocExist.getBody() == null || responseIsDocExist.getBody().contains(docName)) {
			return;
		}
		
		// Step 1: Initiate Resumable Upload Session
		ResponseEntity<String> uploadUrlResponse = retryIfUnauthorizedGdrive(
				() -> gdriveApiCaller.initiateResumableUpload(docName, spendCategoryIdInGdrive),
				"Faild: getting upload url for doc: " + docName, "Success: got upload url for doc: " + docName);
		if (uploadUrlResponse != null) {
			// Step 2: Upload File Content
			String uploadUrl = uploadUrlResponse.getHeaders().getLocation().toString();
			
			retryIfUnauthorizedGdrive(() -> gdriveApiCaller.uploadFileContent(uploadUrl, response),
					"Faild to upload doc: " + docName, "Success: uplading doc: " + docName);
		}
	}

	private String getFolderIdFromGdrive(String folderToFind, String rootFolderId) {
		if (folderToFind == null || rootFolderId == null) {
			log.warn("Faild: folderToFind: {} or rootFolderId: {} is null.", folderToFind, rootFolderId);
			return null;
		}

		JsonNode subFolders = fetchSubFolders(rootFolderId);
		if (subFolders == null) {
			log.warn("Warn: No subfolders found in root folderId: {}", rootFolderId);
			return null;
		}

		JsonNode filesNode = subFolders.get("files");
		if (filesNode.isArray()) {
			for (JsonNode fileNode : filesNode) {
				String id = fileNode.get("id").asText();
				String name = fileNode.get("name").asText();
				if (name.equals(folderToFind)) {
					log.info("Success: Folder {} already exist: ", folderToFind);
					return id;
				}
			}
		}

		return createFolderWithRetry(folderToFind, rootFolderId);
	}

	private JsonNode fetchSubFolders(String rootFolderId) {
		ResponseEntity<String> response = retryIfUnauthorizedGdrive(() -> gdriveApiCaller.listSubFolders(rootFolderId),
				"Faild: to fetch sub folders of root folder: " + rootFolderId,
				"Success: fetched sub folders of root folder: " + rootFolderId);
		if (response != null) {
			return jsonObj.getJson(response.getBody());
		} else {
			return null;
		}
		
	}

	private String createFolderWithRetry(String folderToFind, String rootFolderId) {
		ResponseEntity<String> response = retryIfUnauthorizedGdrive(
				() -> gdriveApiCaller.createFolder(folderToFind, rootFolderId),
				"Faild: Createing Folder(" + folderToFind + ") ", "Success: created Folder(" + folderToFind + ")");
		if (response != null) {
			JsonNode newFolderNode = jsonObj.getJson(response.getBody());
			String folderId = newFolderNode.get("id").asText();
			return folderId;
		} else {
			return null;
		}
	}

	public <T> T retryIfUnauthorizedGdrive(Callable<T> action, String errorMessage, String successMessage) {
		try {
	        return attemptAction(action, errorMessage, successMessage);
	    } catch (Exception e) {
	        if (isUnauthorizedError(e)) {
	            try {
					accessToken.updateAccessToken();
	                // Retry after updating token
	                return attemptAction(action, errorMessage, successMessage);
	            } catch (Exception retryException) {
	                logError(errorMessage, retryException);
	                return null;
	            }
	        } else {
	            logError(errorMessage, e);
	            return null;
	        }
	    }
	}

	public <T> T retryIfUnauthorizedOtcs(Callable<T> action, String errorMessage, String successMessage) {
	    try {
	        return attemptAction(action, errorMessage, successMessage);
	    } catch (Exception e) {
	        if (isUnauthorizedError(e)) {
	            try {
	                otcsTicket.updateOtcsToken();
	                // Retry after updating token
	                return attemptAction(action, errorMessage, successMessage);
	            } catch (Exception retryException) {
	                logError(errorMessage, retryException);
	                return null;
	            }
	        } else {
	            logError(errorMessage, e);
	            return null;
	        }
	    }
	}

	private <T> T attemptAction(Callable<T> action, String errorMessage, String successMessage) throws Exception {
	    T response = action.call();
	    ResponseEntity<?> genericResponse = (ResponseEntity<?>) response;

	    if (genericResponse.getStatusCode() == HttpStatus.OK) {
	        log.info(successMessage);
	        return response;
	    } else {
	    	log.error(errorMessage + " - {}", genericResponse.getStatusCode()); 
	        return null;
	    }
	}

	private void logError(String errorMessage, Exception e) {
	    if (errorMessage != null) {
	        log.error(errorMessage + " - {}", e.getMessage());
	    }
	}

	private boolean isUnauthorizedError(Exception e) {
		return e.getMessage().contains("401 Unauthorized");
	}
}
