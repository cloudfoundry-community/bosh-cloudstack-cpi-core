package com.orange.oss.cloudfoundry.cscpi;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class CPIRestController {
	
	private static Logger logger=LoggerFactory.getLogger(CPIRestController.class.getName());
	
	@RequestMapping("/cpi/")
	public String execute(@RequestParam String request) {
		logger.info("==> received \n {}",request);
		
		//FIXME: wire to CPIImpl
		
		String response="xxx";
		
		return response;
	}


}
