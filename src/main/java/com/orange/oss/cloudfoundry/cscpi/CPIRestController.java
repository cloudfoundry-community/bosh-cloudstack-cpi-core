package com.orange.oss.cloudfoundry.cscpi;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.orange.oss.cloudfoundry.cscpi.domain.CPIRequest;
import com.orange.oss.cloudfoundry.cscpi.domain.CPIResponse;


@RestController
@RequestMapping("/cpi")
public class CPIRestController {
	
	private static Logger logger=LoggerFactory.getLogger(CPIRestController.class.getName());
	
	@RequestMapping(method=RequestMethod.POST,produces="application/json")
	public @ResponseBody CPIResponse execute(@RequestBody CPIRequest request) {
		logger.info("==> received \n {}",request);
		//FIXME: wire to CPIImpl
		
		CPIResponse response=new CPIResponse();
				
		return response;
	}


}
