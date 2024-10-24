package com.supai.app.services.common;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component
@RequiredArgsConstructor
public class JsonObj {
    private static final Logger log = LoggerFactory.getLogger(JsonObj.class);

	private final ObjectMapper objectMapper;

	public JsonNode getJson(String jsonString) {
		try {
			return objectMapper.readTree(jsonString); // Parse the string into JsonNode
		} catch (Exception e) {
			log.error("Failed to parse JSON" + e);
			return null; // Return null if parsing fails
		}
	}
}
