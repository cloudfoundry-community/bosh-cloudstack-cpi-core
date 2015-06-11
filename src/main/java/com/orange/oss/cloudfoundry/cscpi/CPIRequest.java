package com.orange.oss.cloudfoundry.cscpi;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;


@JsonAutoDetect
public class CPIRequest {

	@JsonProperty
	public String method;
	
	@JsonProperty
	public String arguments;
	
	@JsonProperty
	public String context;
}
