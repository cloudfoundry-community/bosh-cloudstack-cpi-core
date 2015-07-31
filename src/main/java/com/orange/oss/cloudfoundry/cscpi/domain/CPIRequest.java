package com.orange.oss.cloudfoundry.cscpi.domain;




import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Base JSON mapping for Director request to CPI
 * @author pierre
 *
 */
@JsonAutoDetect
public class CPIRequest {

	public String method;

	
	public ArrayNode arguments;
	
	public ObjectNode context;
}
