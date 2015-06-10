package com.orange.oss.cloudfoundry;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CPIResponse {

	@JsonProperty
	String result;
	
	@JsonProperty
	String error;
	
	@JsonProperty
	String log;
}
