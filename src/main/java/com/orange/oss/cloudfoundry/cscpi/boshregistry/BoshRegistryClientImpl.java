package com.orange.oss.cloudfoundry.cscpi.boshregistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

public class BoshRegistryClientImpl implements BoshRegistryClient {

	private static Logger logger=LoggerFactory.getLogger(BoshRegistryClientImpl.class.getName());
	

	@Value("${registry.endpoint}")
	String endpoint;


	@Value("${registry.user}")
	String user;

	@Value("${registry.password}")
	String password;
	
}
