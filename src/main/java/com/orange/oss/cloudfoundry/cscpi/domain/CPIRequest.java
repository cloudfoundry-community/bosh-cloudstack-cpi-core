package com.orange.oss.cloudfoundry.cscpi.domain;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;


@JsonAutoDetect
public class CPIRequest {

	public String method;
	
	public ArrayNode arguments;
	
	public ObjectNode context;
}
