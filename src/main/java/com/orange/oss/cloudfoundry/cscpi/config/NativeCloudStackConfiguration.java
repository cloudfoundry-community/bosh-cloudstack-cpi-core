package com.orange.oss.cloudfoundry.cscpi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.orange.oss.cloudfoundry.cspi.cloudstack.NativeCloudstackConnector;
import com.orange.oss.cloudfoundry.cspi.cloudstack.NativeCloudstackConnectorImpl;

@Configuration
public class NativeCloudStackConfiguration {

	@Bean
	NativeCloudstackConnector nativeCloudstackConnector() {
		NativeCloudstackConnectorImpl ncc = new NativeCloudstackConnectorImpl();
		return ncc;

	}

}
