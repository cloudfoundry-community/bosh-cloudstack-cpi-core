package com.orange.oss.cloudfoundry.cscpi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.orange.oss.cloudfoundry.cscpi.logic.BoshRegistryClient;
import com.orange.oss.cloudfoundry.cscpi.logic.BoshRegistryClientImpl;

@Configuration
public class BoshRegistryClientConfig {
	
	@Bean
	public BoshRegistryClient boshRegistryClient(){
		return new BoshRegistryClientImpl();
	}
	
}
