package com.orange.oss.cloudfoundry.cscpi.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonObject;

public class CPIResponse {

	JsonNode result;
	
	JsonObject error;
	
	String log;
}
