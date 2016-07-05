package com.orange.oss.cloudfoundry.cscpi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.orange.oss.cloudfoundry.cspi.cloudstack.CacheableCloudstackConnector;

@Configuration
public class CacheableCloudStackConnectorConfiguration {

	@Bean
	CacheableCloudstackConnector cloudstackConnector() {
		CacheableCloudstackConnector ccc = new CacheableCloudstackConnector();
		return ccc;

	}

}
