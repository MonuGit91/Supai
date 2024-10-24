package com.supai.app.services.otcs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.supai.app.config.OtcsCategory;
import com.supai.app.config.OtcsCredentials;
import com.supai.app.models.SearchDetails;
import com.supai.app.services.common.JsonObj;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class SearchDocs {
	private static final Logger log = LoggerFactory.getLogger(JsonObj.class);
	@Autowired
	private RestTemplate restTemplate;
	@Autowired
	private JsonObj jsonObj;
	@Autowired
	private OtcsCredentials otcsCredentials;
	@Autowired
	private OtcsTicket otcsTicket;
	@Autowired
	private Environment environment;
	@Autowired 
	private OtcsCategory otcsCategory;
	@Autowired
    private ApplicationContext applicationContext;

	public List<SearchDetails> searchAllDocs() {
		Map<String, String> attrMap = otcsCategory.getAttr();
		Map<String, String> spendCategoryMap = otcsCategory.getSpendCategory();
		
		List<SearchDetails> subCattegoryWiseSearches = new ArrayList<>();
		for(Entry<String, String> attrEntry : attrMap.entrySet()) {
			String attr = attrEntry.getValue(); // Attr_749892_3
			
			for(Entry<String, String> subCategoryEntry : spendCategoryMap.entrySet()) {
				String subCategory = subCategoryEntry.getValue(); // Software
		        SearchDetails searchDetails = applicationContext.getBean(SearchDetails.class);
		        searchDetails.setAttribute(attr);
		        searchDetails.setSpendCategory(subCategory);
		        try {
					searchDetails.setSearchResult(callApi(attr, subCategory));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					if(e.getMessage().contains("401 Unauthorized")) {
						otcsTicket.updateOtcsToken();
						try {
							searchDetails.setSearchResult(callApi(attr, subCategory));
						} catch (Exception e1) {
							// TODO Auto-generated catch block
					        log.error("{}", e1);
						}
					}
				}
				subCattegoryWiseSearches.add(searchDetails);
			}
		}
		
		return subCattegoryWiseSearches;
	}

	private JsonNode callApi(String attr, String subCategory) throws Exception{
		// TODO Auto-generated method stub
		String url = environment.getProperty("otcs.api.search-docs");
		String bodyFromat = "[qlregion attr] (subAttr) and OTSubType:144";

		HttpHeaders headers = new HttpHeaders();
		headers.add("otcsticket", otcsCredentials.getOtcsticket());
		headers.add("Content-Type", "application/x-www-form-urlencoded");

		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("where", bodyFromat.replace("attr", attr).replace("subAttr", subCategory));
		
		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
		
		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

		return jsonObj.getJson(response.getBody());		
	}
}
