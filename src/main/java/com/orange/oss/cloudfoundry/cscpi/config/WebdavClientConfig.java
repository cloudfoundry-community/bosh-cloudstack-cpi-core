package com.orange.oss.cloudfoundry.cscpi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.orange.oss.cloudfoundry.cscpi.webdav.WebdavServerAdapterImpl;

@Configuration
public class WebdavClientConfig {
	
	@Bean
	public WebdavServerAdapterImpl webdavClient(){
		return new WebdavServerAdapterImpl();
	}
	
}
