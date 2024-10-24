package com.supai.app.models;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.Data;

@Data
@Component
@Scope("prototype")
public class SearchDetails {
	private String attribute;
	private String  spendCategory;
	private JsonNode searchResult;
}
