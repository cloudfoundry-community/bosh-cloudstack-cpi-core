package com.orange.oss.cloudfoundry.cscpi;


import java.util.Properties;

import org.jclouds.Constants;
import org.jclouds.ContextBuilder;
import org.jclouds.cloudstack.CloudStackApi;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;



@Configuration
public class CloudStackConfiguration {

	private final static Logger logger=LoggerFactory.getLogger(CloudStackConfiguration.class.getName());
	
	
	
	@Value("${cloudstack.endpoint}")	
	String endpoint;

	@Value("${cloudstack.api_key}")	
	String api_key;

	@Value("${cloudstack.secret_access_key}")	
	String secret_access_key;

	@Value("${cloudstack.default_key_name}")	
	String default_key_name;

	@Value("${cloudstack.private_key}")	
	String private_key;

	
	
	@Bean
		public CloudStackApi cloudStackAdapter(){
		
		String username=this.api_key;
		String password=this.secret_access_key;
		
		logger.debug("cloudstack adapter. endpoint {} \n username {} \n",endpoint,username);
		
        Iterable<Module> modules = ImmutableSet.<Module>of(new SLF4JLoggingModule());

        logger.debug("initialize jclouds compute API");
        String provider = "cloudstack";
        
        logger.debug("logging as {}",username);
        
       Properties overrides = new Properties();
        overrides.setProperty(Constants.PROPERTY_RELAX_HOSTNAME, "true");
        overrides.setProperty(Constants.PROPERTY_TRUST_ALL_CERTS, "true");
        
        CloudStackApi api = ContextBuilder.newBuilder(provider)
                .endpoint(endpoint)
                .credentials(username, password)
                .modules(modules)
                .overrides(overrides)
                .buildApi(CloudStackApi.class);
        return api;

	}
	
	
}
