package com.orange.oss.cloudfoundry.cscpi;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.orange.oss.cloudfoundry.cscpi.domain.CPIResponse;


@RestController
@RequestMapping("/cpi")
public class CPIRestController {
	
	private static Logger logger=LoggerFactory.getLogger(CPIRestController.class.getName());
	
	@Autowired
	private CPIAdapter cpiAdaper;
	
	
	@RequestMapping(method=RequestMethod.POST,produces="application/json")
	public @ResponseBody JsonNode execute(@RequestBody JsonNode  request) {
		logger.info("==> received \n {}",request);
		CPIResponse response=this.cpiAdaper.execute(request);
		logger.info("cpi-core response : {}",response);
		
		
		JsonNodeFactory nodeFactory = new JsonNodeFactory() {
		};

		ObjectNode node = nodeFactory.objectNode();
		ObjectNode child = nodeFactory.objectNode(); // the child
		child.put("result", response.result.get(0));
		child.put("error", response.error);
		child.put("log", response.log);
		
		logger.info("generated json response payload {}",node.asText());
		
		return node;

	}


}
