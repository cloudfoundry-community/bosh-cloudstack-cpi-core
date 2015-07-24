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
import com.orange.oss.cloudfoundry.cscpi.domain.CPIResponse;


@RestController
@RequestMapping("/cpi")
public class CPIRestController {
	
	private static Logger logger=LoggerFactory.getLogger(CPIRestController.class.getName());
	
	@Autowired
	private CPIAdapter cpiAdaper;
	
	
	@RequestMapping(method=RequestMethod.POST,produces="application/json")
	public @ResponseBody CPIResponse execute(@RequestBody JsonNode  request) {
		logger.info("==> received \n {}",request);
		return this.cpiAdaper.execute(request);

	}


}
