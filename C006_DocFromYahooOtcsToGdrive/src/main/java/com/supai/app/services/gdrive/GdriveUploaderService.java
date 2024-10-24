package com.supai.app.services.gdrive;

import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.supai.app.models.SearchDetails;
import com.supai.app.services.common.JsonObj;
import com.supai.app.services.otcs.DownloadDoc;
import com.supai.app.services.otcs.OtcsApiCall;
import com.supai.app.services.otcs.OtcsTicket;
import com.supai.app.services.otcs.SearchDocs;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GdriveUploaderService {
	private static final Logger log = LoggerFactory.getLogger(JsonObj.class);
	private final OtcsApiCall otcsApiCall;
	private final SearchDocs searchDocs;
	private final DownloadDoc downloadDoc;
	private final Environment environment;
	private final GdriveApiCaller gdriveApiCaller;
	private final AccessToken accessToken;
	private final OtcsTicket otcsTicket;

	public void upload() {
		List<SearchDetails> subCattegoryWiseSearches = searchDocs.searchAllDocs();

		for (SearchDetails result : subCattegoryWiseSearches) {
			String categoryId = getCatogeryId(result.getAttribute());
			String spendCategory = result.getSpendCategory();
			JsonNode searched = result.getSearchResult();
			parseAndUpload(categoryId, spendCategory, searched);
		}
	}

	private void parseAndUpload(String categoryId, String spendCategory, JsonNode searched) {

		JsonNode resultsNode = searched.path("results");

		if (resultsNode.isArray()) {
			for (JsonNode result : resultsNode) {
				JsonNode propertiesNode = result.path("data").path("properties");

				String docId = propertiesNode.path("id").asText();
				String docName = propertiesNode.path("name").asText();

				JsonNode categoryDetails = retryIfUnauthorizedGdrive(() -> otcsApiCall.getCategoryDetails(docId, categoryId), "error", "success");

				if (categoryDetails == null) {
					log.warn("Category details not found for docId: {}, categoryId: {}", docId, categoryId);
					continue;
				}

				String categoryName = categoryDetails.path("definitions").path(categoryId).path("name").asText();

				ResponseEntity<byte[]> response = retryIfUnauthorizedOtcs(() -> downloadDoc.GetDoc(Integer.parseInt(docId), docName), "error", "success");
				if (response != null) {
					log.info("Document downloaded successfully: {}", docName);
					uploadToGdrive(response, categoryName, spendCategory, docName);
				} 
			}
		} else {
			log.info("No results found for the search query.");
		}
	}

	private void uploadToGdrive(ResponseEntity<byte[]> response, String categoryName, String spendCategory,
			String docName) {

		String categoryIdInGdrive = getFolderIdFromGdrive(categoryName,
				environment.getProperty("gdrive.folder.root-Test"));
		String spendCategoryIdInGdrive = getFolderIdFromGdrive(spendCategory, categoryIdInGdrive);

		boolean isDocAlreadyUploaded = retryIfUnauthorizedGdrive(
				() -> gdriveApiCaller.isDocPresentOnGdrive(docName, spendCategoryIdInGdrive), "error", "success");
		if (isDocAlreadyUploaded) {
			return;
		}

		// Step 1: Initiate Resumable Upload Session
		String uploadUrl = retryIfUnauthorizedGdrive(
				() -> gdriveApiCaller.initiateResumableUpload(docName, spendCategoryIdInGdrive), "error", "success");
		if (uploadUrl != null) {
			// Step 2: Upload File Content
			retryIfUnauthorizedGdrive(() -> gdriveApiCaller.uploadFileContent(uploadUrl, response), "error", "success");
		}

	}

	private String getFolderIdFromGdrive(String folderToFind, String rootFolderId) {
		if (folderToFind == null || rootFolderId == null) {
			log.warn("folderToFind or rootFolderId is null. folderToFind: {}, rootFolderId: {}", folderToFind,
					rootFolderId);
			return null;
		}

		JsonNode subFolders = fetchSubFoldersWithRetry(rootFolderId);
		if (subFolders == null) {
			log.warn("No subfolders found in root folder: {}", rootFolderId);
			return null;
		}

		JsonNode filesNode = subFolders.get("files");
		if (filesNode.isArray()) {
			for (JsonNode fileNode : filesNode) {
				String id = fileNode.get("id").asText();
				String name = fileNode.get("name").asText();
				if (name.equals(folderToFind)) {
					log.info("Found folder: {} with id: {}", folderToFind, id);
					return id;
				}
			}
		}

		return createFolderWithRetry(folderToFind, rootFolderId);
	}

	private JsonNode fetchSubFoldersWithRetry(String rootFolderId) {
		return retryIfUnauthorizedGdrive(() -> gdriveApiCaller.listSubFolders(rootFolderId), "erroe", "success");
	}

	private String createFolderWithRetry(String folderToFind, String rootFolderId) {
		JsonNode newFolderNode = retryIfUnauthorizedGdrive(() -> gdriveApiCaller.createFolder(folderToFind, rootFolderId),
				"erroe", "success");
		if (newFolderNode != null) {
			String folderId = newFolderNode.get("id").asText();
			return folderId;
		} else {
			return null;
		}
	}

	public <T> T retryIfUnauthorizedGdrive(Callable<T> action, String errorMessage, String successMessage) {
		try {
			// First attempt
			T response = action.call();
			log.info(successMessage);
			return response;
		} catch (Exception e) {
			if (isUnauthorizedError(e)) {
				accessToken.updateAccessToken();
				try {
					// Retry after updating token
					return action.call();
				} catch (Exception retryException) {
					log.error(errorMessage, retryException);
					return null;
				}
			} else {
				log.error(errorMessage, e);
				return null;
			}
		}
	}

	public <T> T retryIfUnauthorizedOtcs(Callable<T> action, String errorMessage, String successMessage) {
		try {
			// First attempt
			T response = action.call();
			log.info(successMessage);
			return response;
		} catch (Exception e) {
			if (isUnauthorizedError(e)) {
				otcsTicket.updateOtcsToken();
				try {
					// Retry after updating token
					return action.call();
				} catch (Exception retryException) {
					log.error(errorMessage, retryException);
					return null;
				}
			} else {
				log.error(errorMessage, e);
				return null;
			}
		}
	}

	
	private boolean isUnauthorizedError(Exception e) {
		return e.getMessage().contains("401 Unauthorized");
	}

	private String getCatogeryId(String attribute) {
		// TODO Auto-generated method stub
		String id = attribute.substring(attribute.indexOf('_') + 1, attribute.lastIndexOf('_'));
		return id;
	}
}
